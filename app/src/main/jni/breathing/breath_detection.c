/**
 *
 */

#include <malloc.h>
#include "breath_detection.h"
#include "breathing.h"

void initialise_rms_threshold_buffer(ThresholdBuffer *threshold_buffer, unsigned int threshold_filter_size) {
    threshold_buffer->threshold_filter_size = threshold_filter_size;
    threshold_buffer->fill = 0;
    threshold_buffer->current_position = -1;
    threshold_buffer->is_valid = false;
    threshold_buffer->lower_values_sum = 0;
    threshold_buffer->upper_values_sum = 0;
    threshold_buffer->upper_values_sum_fill = 0;
    threshold_buffer->lower_values_sum_fill = 0;
    threshold_buffer->upper_threshold_value = NAN;
    threshold_buffer->lower_threshold_value = NAN;

    threshold_buffer->values = calloc(threshold_filter_size, sizeof(float *));
    threshold_buffer->values_type = calloc(threshold_filter_size, sizeof(float *));

    for (int i = 0; i < threshold_filter_size; i++) {
        threshold_buffer->values_type[i] = INVALID;
        threshold_buffer->values[i] = 0;
    }
}

void update_rms_threshold(float breathing_signal_value, ThresholdBuffer *threshold_buffer) {

    // Increment position
    threshold_buffer->current_position =
            (threshold_buffer->current_position + 1) % threshold_buffer->threshold_filter_size;

    // Overwrite value at current position by first deleting that value from the corresponding sum
    if (threshold_buffer->values_type[threshold_buffer->current_position] == POSITIVE) {
        threshold_buffer->upper_values_sum -= threshold_buffer->values[threshold_buffer->current_position];
        threshold_buffer->upper_values_sum_fill--;
    } else if (threshold_buffer->values_type[threshold_buffer->current_position] == NEGATIVE) {
        threshold_buffer->lower_values_sum -= threshold_buffer->values[threshold_buffer->current_position];
        threshold_buffer->lower_values_sum_fill--;
    }

    if (isnan(breathing_signal_value) == true) {
        threshold_buffer->values_type[threshold_buffer->current_position] = INVALID;
    } else {
        float squared_value = breathing_signal_value * breathing_signal_value;
        threshold_buffer->values[threshold_buffer->current_position] = squared_value;

        // Add the squared breathing signal value to the right sum (upper or lower)
        if (breathing_signal_value >= 0) {
            threshold_buffer->upper_values_sum_fill++;
            threshold_buffer->values_type[threshold_buffer->current_position] = POSITIVE;
            threshold_buffer->upper_values_sum += squared_value;
        } else {
            threshold_buffer->lower_values_sum_fill++;
            threshold_buffer->values_type[threshold_buffer->current_position] = NEGATIVE;
            threshold_buffer->lower_values_sum += squared_value;
        }
    }

    if (threshold_buffer->fill < threshold_buffer->threshold_filter_size) {
        threshold_buffer->fill++;
    }

    if (threshold_buffer->fill < threshold_buffer->threshold_filter_size) {
        threshold_buffer->is_valid = false;
        return;
    }

    // calculate current upper threshold
    if (threshold_buffer->upper_values_sum_fill > 0) {
        // Calculate the root mean square
        threshold_buffer->upper_threshold_value = (float) sqrt(
                threshold_buffer->upper_values_sum / threshold_buffer->upper_values_sum_fill);
    } else {
        threshold_buffer->upper_threshold_value = NAN;
    }

    // calculate current lower threshold
    if (threshold_buffer->lower_values_sum_fill > 0) {
        // Calculate the root mean square
        threshold_buffer->lower_threshold_value = (float) -sqrt(
                threshold_buffer->lower_values_sum / threshold_buffer->lower_values_sum_fill);
    } else
        threshold_buffer->lower_threshold_value = NAN;

    threshold_buffer->is_valid = true;
}

void initialise_breath(CurrentBreath *breath, float lower_threshold_limit, float upper_threshold_limit,
                       float sampling_frequency) {
    breath->state = UNKNOWN;
    breath->breathing_rate = NAN;
    breath->min_threshold = lower_threshold_limit;
    breath->max_threshold = upper_threshold_limit;
    breath->sample_count = 0;
    breath->sampling_frequency = sampling_frequency;
    breath->is_current_breath_valid = false;
    breath->is_complete = false;
    // We assume by default that the inspiration is above the x-axis. Adjust this if several breaths indicate
    // otherwise (i.e. shorter inspiration part below x).
    breath->is_inspiration_above_x = true;
    breath->first_part_length = 0;
    breath->count_abnormal_breaths = 0;
}

void end_breath(CurrentBreath *breath) {
    if (breath->is_current_breath_valid) {
        // If the first part of the breath was longer than the second, this breath counts as abnormal.
        if (breath->first_part_length > breath->sample_count - breath->first_part_length) {
            breath->count_abnormal_breaths += 1;
        } else {
            // Reset count
            breath->count_abnormal_breaths = 0;
        }

        // If we have three abnormal breaths, the breath detection is "flipped", i.e. inspiration is above x axis
        // instead of below or other way around
        if (breath->count_abnormal_breaths >= NUMBER_OF_ABNORMAL_BREATHS_SWITCH) {
            breath->count_abnormal_breaths = 0;
            breath->is_inspiration_above_x = !breath->is_inspiration_above_x;
        } else {
            // Only when we didn't have 3 abnormal breaths in a row do we count this breath as valid.
            // Calculate the breathing rate of the last cycle
            float new_breathing_rate = (float) (60.0 * breath->sampling_frequency / (float) breath->sample_count);

            // We want the breathing rate to lie in a realistic range
            if (new_breathing_rate >= LOWEST_POSSIBLE_BREATHING_RATE &&
                new_breathing_rate <= HIGHEST_POSSIBLE_BREATHING_RATE) {
                // Current breathing rate is valid -> Store it!
                breath->breathing_rate = new_breathing_rate;
                // Signal to caller that the breathing rate has changed
                breath->is_complete = true;
            }
        }
    }
    breath->sample_count = 0;
    breath->first_part_length = 0;
    breath->is_current_breath_valid = true;
}

void update_breath(float breathing_signal, float upper_threshold,
                   float lower_threshold, CurrentBreath *breath) {
    breath->sample_count++;

    if (isnan(upper_threshold) || isnan(lower_threshold) || isnan(breathing_signal)) {
        breath->breathing_rate = NAN;
        breath->is_current_breath_valid = false;
        return;
    }

    // Set initial state, if required
    if (breath->state == UNKNOWN) {
        if (breathing_signal < lower_threshold) {
            breath->state = LOW;
        } else if (breathing_signal > upper_threshold) {
            breath->state = HIGH;
        } else {
            breath->state = MID_UNKNOWN;
        }
    }

    // The sum of the absolute threshold values has to lie above 2 * min_threshold
    if (upper_threshold - lower_threshold < breath->min_threshold * 2.0f) {
        breath->state = UNKNOWN;
        breath->breathing_rate = NAN;
        breath->is_current_breath_valid = false;
        return;
    }


    // The sum of the absolute threshold values has to lie below 2 * max_threshold
    if (upper_threshold - lower_threshold > breath->max_threshold * 2.0f) {
        breath->state = UNKNOWN;
        breath->breathing_rate = NAN;
        breath->is_current_breath_valid = false;
        return;
    }

    if (breath->state == LOW && breathing_signal > lower_threshold) {
        breath->state = MID_RISING;
    } else if (breath->state == HIGH && breathing_signal < upper_threshold) {
        breath->state = MID_FALLING;
    } else if ((breath->state == MID_RISING || breath->state == MID_UNKNOWN) &&
               breathing_signal > upper_threshold) {
        breath->state = HIGH;

        // If inspiration is above x axis, the breath has now ended
        if (breath->is_inspiration_above_x) {
            end_breath(breath);
        } else {
            // The first part of the breath is over -> note down length
            breath->first_part_length = breath->sample_count;
        }
    } else if ((breath->state == MID_FALLING || breath->state == MID_UNKNOWN) &&
               breathing_signal < lower_threshold) {
        breath->state = LOW;

        // If inspiration is above x axis, the breath has now ended
        if (breath->is_inspiration_above_x) {
            // The first part of the breath is over -> note down length
            breath->first_part_length = breath->sample_count;
        } else {
            end_breath(breath);
        }
    }
}

void update_sampling_frequency(float sampling_frequency, CurrentBreath *breath) {
    breath->sampling_frequency = sampling_frequency;
}

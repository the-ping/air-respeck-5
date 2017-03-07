/**
 *
 */

#include "breath_detection.h"
#include "breathing.h"

void initialise_rms_threshold_buffer(ThresholdBuffer *threshold_buffer) {
    threshold_buffer->fill = 0;
    threshold_buffer->current_position = 0;
    threshold_buffer->is_valid = false;
    threshold_buffer->lower_values_sum = 0;
    threshold_buffer->upper_values_sum = 0;
    threshold_buffer->upper_values_sum_fill = 0;
    threshold_buffer->lower_values_sum_fill = 0;

    for (int i = 0; i < THRESHOLD_FILTER_SIZE; i++)
        threshold_buffer->values_type[i] = INVALID;
}

void update_rms_threshold(double breathing_signal_value, ThresholdBuffer *threshold_buffer) {

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
        double squared_value = breathing_signal_value * breathing_signal_value;
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

    threshold_buffer->current_position = (threshold_buffer->current_position + 1) % THRESHOLD_FILTER_SIZE;

    if (threshold_buffer->fill < THRESHOLD_FILTER_SIZE) {
        threshold_buffer->fill++;
    }

    if (threshold_buffer->fill < THRESHOLD_FILTER_SIZE) {
        threshold_buffer->is_valid = false;
        return;
    }

    // calculate current upper threshold
    if (threshold_buffer->upper_values_sum_fill > 0) {
        // Calculate the root mean square
        threshold_buffer->upper_threshold_value = sqrt(
                threshold_buffer->upper_values_sum / threshold_buffer->upper_values_sum_fill);
    } else {
        threshold_buffer->upper_threshold_value = NAN;
    }

    // calculate current lower threshold
    if (threshold_buffer->lower_values_sum_fill > 0) {
        // Calculate the root mean square
        threshold_buffer->lower_threshold_value = -sqrt(
                threshold_buffer->lower_values_sum / threshold_buffer->lower_values_sum_fill);
    }
    else
        threshold_buffer->lower_threshold_value = NAN;

    threshold_buffer->is_valid = true;
}

void initialise_breath(CurrentBreath *breath) {
    breath->state = UNKNOWN;
    breath->breathing_rate = NAN;
    breath->min_threshold = 0.01;
    breath->max_threshold = 0.5;
    breath->sample_count = 0;
    breath->is_sample_count_valid = false;
    breath->is_complete = false;
}

void update_breath(double breathing_signal, double upper_threshold,
                   double lower_threshold, CurrentBreath *breath) {
    breath->sample_count++;

    if (isnan(upper_threshold) || isnan(lower_threshold) || isnan(breathing_signal)) {
        breath->breathing_rate = NAN;
        breath->is_sample_count_valid = false;
        return;
    }

    // set initial state, if required
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
        breath->is_sample_count_valid = false;
        return;
    }

    /*
    // The sum of the absolute threshold values has to lie below 2 * max_threshold
    if (upper_threshold - lower_threshold > breath->max_threshold * 2.0f) {
        breath->state = UNKNOWN;
        breath->bpm = NAN;
        breath->is_sample_count_valid = false;
        return;
    }
    */

    if (breath->state == LOW && breathing_signal > lower_threshold) {
        breath->state = MID_RISING;
    }
    else if (breath->state == HIGH && breathing_signal < upper_threshold) {
        breath->state = MID_FALLING;
    }
    else if ((breath->state == MID_RISING || breath->state == MID_UNKNOWN) &&
             breathing_signal > upper_threshold) {
        breath->state = HIGH;
    }
    else if ((breath->state == MID_FALLING || breath->state == MID_UNKNOWN) &&
             breathing_signal < lower_threshold) {
        breath->state = LOW;

        // A full breath cycle is finished. Calculate the breathing rate of the last cycle
        if (breath->is_sample_count_valid) {
            breath->breathing_rate =
                    60.0 * SAMPLE_RATE / (float) breath->sample_count;
        }

        breath->sample_count = 0;
        breath->is_sample_count_valid = true;
        breath->is_complete = true;
    }
}
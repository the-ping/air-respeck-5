/**
 *
 */
#include "breathing_rate_stats.h"
#include <math.h>

void initialise_breathing_rate_buffer(BreathingRateBuffer *breathing_rate_buffer) {
    breathing_rate_buffer->fill = 0;
    breathing_rate_buffer->is_valid = false;
}

int ascending_compare_function(const void *a, const void *b) {
    float *x = (float *) a;
    float *y = (float *) b;
    if (*x < *y) {
        return -1;
    } else if (*x > *y) {
        return 1;
    }
    return 0;
}

void update_breathing_rate_buffer(float breathing_rate, BreathingRateBuffer *breathing_rate_buffer) {
    if (breathing_rate_buffer->fill < BREATHING_RATES_BUFFER_SIZE) {
        breathing_rate_buffer->breathing_rates[breathing_rate_buffer->fill] = breathing_rate;
        breathing_rate_buffer->fill++;
    }
}

void calculate_breathing_rate_stats(BreathingRateBuffer *breathing_rate_buffer) {
    // Sort the values in the breathing_rate_buffer
    qsort(breathing_rate_buffer->breathing_rates, breathing_rate_buffer->fill,
          sizeof(breathing_rate_buffer->breathing_rates[0]), ascending_compare_function);

    float one_breathing_rate = breathing_rate_buffer->breathing_rates[DISCARD_LOWER_BREATHING_RATES];
    breathing_rate_buffer->previous_mean = breathing_rate_buffer->current_mean = one_breathing_rate;
    breathing_rate_buffer->previous_variance = 0.0;
    breathing_rate_buffer->max = one_breathing_rate;
    breathing_rate_buffer->min = one_breathing_rate;

    // Go through the breathing rates. We ignore some of the smallest and largest values,
    // as determined by DISCARD_LOWER_BREATHING_RATES and DISCARD_UPPER_BREATHING_RATES
    for (int i = DISCARD_LOWER_BREATHING_RATES + 1;
         i < breathing_rate_buffer->fill - DISCARD_UPPER_BREATHING_RATES; i++) {
        one_breathing_rate = breathing_rate_buffer->breathing_rates[i];

        // Update running mean
        breathing_rate_buffer->current_mean = breathing_rate_buffer->previous_mean +
                                      (one_breathing_rate - breathing_rate_buffer->previous_mean) /
                                      breathing_rate_buffer->fill;
        // Update running variance (https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance)
        breathing_rate_buffer->current_variance = breathing_rate_buffer->previous_variance +
                                      (one_breathing_rate - breathing_rate_buffer->previous_mean) *
                                      (one_breathing_rate - breathing_rate_buffer->current_mean);

        breathing_rate_buffer->previous_mean = breathing_rate_buffer->current_mean;
        breathing_rate_buffer->previous_variance = breathing_rate_buffer->current_variance;

        // Update min and max breathing rate
        breathing_rate_buffer->max = (one_breathing_rate > breathing_rate_buffer->max) ? one_breathing_rate
                                                                                       : breathing_rate_buffer->max;
        breathing_rate_buffer->min = (one_breathing_rate < breathing_rate_buffer->min) ? one_breathing_rate
                                                                                       : breathing_rate_buffer->min;
        breathing_rate_buffer->is_valid = true;
    }
}

int breathing_rate_number_of_breaths(BreathingRateBuffer *breathing_rate_buffer) {
    return breathing_rate_buffer->fill;
}

float breathing_rate_mean(BreathingRateBuffer *breathing_rate_buffer) {
    return (breathing_rate_buffer->fill > 0) ? breathing_rate_buffer->current_mean : NAN;
}

float breathing_rate_variance(BreathingRateBuffer *breathing_rate_buffer) {
    return ((breathing_rate_buffer->fill > 1) ? breathing_rate_buffer->current_variance /
                                                       (breathing_rate_buffer->fill - 1) : 0.0f);
}

float breathing_rate_standard_deviation(BreathingRateBuffer *breathing_rate_buffer) {
    return sqrt(breathing_rate_variance(breathing_rate_buffer));
}

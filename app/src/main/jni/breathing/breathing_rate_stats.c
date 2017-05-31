/**
 *
 */
#include "breathing_rate_stats.h"
#include <math.h>


void initialise_breathing_rate_stats(BreathingRateStats *breathing_rate_stats) {
    breathing_rate_stats->fill = 0;
    breathing_rate_stats->is_valid = false;
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

void update_breathing_rate_stats(float breathing_rate, BreathingRateStats *breathing_rate_stats) {
    if (breathing_rate_stats->fill < BREATHING_RATES_BUFFER_SIZE) {
        breathing_rate_stats->breathing_rates[breathing_rate_stats->fill] = breathing_rate;
        breathing_rate_stats->fill++;
    }
}

void calculate_breathing_rate_stats(BreathingRateStats *breathing_rate_stats) {
    // Sort the breathing rates
    qsort(breathing_rate_stats->breathing_rates, breathing_rate_stats->fill,
          sizeof(breathing_rate_stats->breathing_rates[0]), ascending_compare_function);

    // If the breathing buffer is lower than what we discard at the edges, return NaN
    if (breathing_rate_stats->fill <= DISCARD_LOWER_BREATHING_RATES + DISCARD_UPPER_BREATHING_RATES) {
        breathing_rate_stats->is_valid = false;
        return;
    }

    float one_breathing_rate = breathing_rate_stats->breathing_rates[DISCARD_LOWER_BREATHING_RATES];
    breathing_rate_stats->previous_mean = breathing_rate_stats->current_mean = one_breathing_rate;
    breathing_rate_stats->previous_variance = 0.0;
    breathing_rate_stats->max = one_breathing_rate;
    breathing_rate_stats->min = one_breathing_rate;

    // Go through the breathing rates. We ignore some of the smallest and largest values,
    // as determined by DISCARD_LOWER_BREATHING_RATES and DISCARD_UPPER_BREATHING_RATES
    for (int i = DISCARD_LOWER_BREATHING_RATES;
         i < breathing_rate_stats->fill - DISCARD_UPPER_BREATHING_RATES; i++) {
        one_breathing_rate = breathing_rate_stats->breathing_rates[i];

        // Update running mean
        breathing_rate_stats->current_mean = breathing_rate_stats->previous_mean +
                                              (one_breathing_rate - breathing_rate_stats->previous_mean) /
                                              breathing_rate_stats->fill;
        // Update running variance (https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance)
        breathing_rate_stats->current_variance = breathing_rate_stats->previous_variance +
                                                  (one_breathing_rate - breathing_rate_stats->previous_mean) *
                                                  (one_breathing_rate - breathing_rate_stats->current_mean);

        breathing_rate_stats->previous_mean = breathing_rate_stats->current_mean;
        breathing_rate_stats->previous_variance = breathing_rate_stats->current_variance;

        // Update min and max breathing rate
        breathing_rate_stats->max = (one_breathing_rate > breathing_rate_stats->max) ? one_breathing_rate
                                                                                       : breathing_rate_stats->max;
        breathing_rate_stats->min = (one_breathing_rate < breathing_rate_stats->min) ? one_breathing_rate
                                                                                       : breathing_rate_stats->min;
    }
    breathing_rate_stats->is_valid = true;
}

int breathing_rate_number_of_breaths(BreathingRateStats *breathing_rate_buffer) {
    return breathing_rate_buffer->fill;
}

float breathing_rate_mean(BreathingRateStats *breathing_rate_stats) {
    return (breathing_rate_stats->is_valid == true) ? breathing_rate_stats->current_mean : NAN;
}

float breathing_rate_variance(BreathingRateStats *breathing_rate_stats) {
    return ((breathing_rate_stats->is_valid == true) ? breathing_rate_stats->current_variance /
                                                       (breathing_rate_stats->fill - 1) : NAN);
}

float breathing_rate_standard_deviation(BreathingRateStats *breathing_rate_stats) {
    return (float) ((breathing_rate_stats->is_valid == true) ?
                    sqrt(breathing_rate_variance(breathing_rate_stats)) : NAN);
}

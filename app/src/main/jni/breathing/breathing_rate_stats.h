/**
 *
 */

#ifndef __ma_stats_h
#define __ma_stats_h

#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>

#define BREATHING_RATES_BUFFER_SIZE 50
#define DISCARD_UPPER_BREATHING_RATES 2
#define DISCARD_LOWER_BREATHING_RATES 2

typedef struct {
    int fill;
    float breathing_rates[BREATHING_RATES_BUFFER_SIZE];
    bool is_valid;
    float previous_mean;
    float current_mean;
    float previous_variance;
    float current_variance;
    float max;
    float min;
} BreathingRateStats;

void initialise_breathing_rate_stats(BreathingRateStats *breathing_rate_stats);

void update_breathing_rate_stats(float breathing_rate, BreathingRateStats *breathing_rate_stats);

void calculate_breathing_rate_stats(BreathingRateStats *breathing_rate_stats);

int breathing_rate_number_of_breaths(BreathingRateStats *breathing_rate_buffer);

float breathing_rate_mean(BreathingRateStats *breathing_rate_buffer);

float breathing_rate_variance(BreathingRateStats *breathing_rate_stats);

float breathing_rate_standard_deviation(BreathingRateStats *breathing_rate_stats);

#endif

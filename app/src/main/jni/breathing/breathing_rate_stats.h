/**
 *
 */

#ifndef __ma_stats_h
#define __ma_stats_h

#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>

#define BREATHING_RATES_BUFFER_SIZE 32
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
} BreathingRateBuffer;

void initialise_breathing_rate_stats(BreathingRateBuffer *breathing_rate_buffer);

void update_breathing_rate_buffer(float breathing_rate, BreathingRateBuffer *breathing_rate_buffer);

void calculate_breathing_rate_stats(BreathingRateBuffer *breathing_rate_buffer);

int breathing_rate_number_of_breaths(BreathingRateBuffer *breathing_rate_buffer);

float breathing_rate_mean(BreathingRateBuffer *breathing_rate_buffer);

float breathing_rate_variance(BreathingRateBuffer *breathing_rate_buffer);

float breathing_rate_standard_deviation(BreathingRateBuffer *breathing_rate_buffer);

#endif

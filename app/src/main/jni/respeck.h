#ifndef __RESPECK_H__
#define __RESPECK_H__

#include <stdbool.h>

char *testMessage();

/**
 *
 * @param env
 * @param this
 * @param is_post_filtering_enabled Whether the breathing signal should be filtered in the end. Should be yes by default.
 * @param activity_cutoff The acitivity level at which a breathing signal is invalidated due to moving.
 * @param threshold_filter_size The size of the threshold filter (buffer) which is used to determine threshold crossings
 * of the breathing signal
 * @param lower_threshold_limit The minimum value the threshold is allowed to take
 * @param upper_threshold_limit The maximum value the threshold is allowed to take
 * @param threshold_factor The factor with which to multiply the RMS threshold before considering it for minimum/
 * maximum limit and for the crossings
 */
void initBreathing(bool is_post_filtering_enabled, float activity_cutoff, unsigned int threshold_filter_size,
                   float lower_threshold_limit, float upper_threshold_limit, float threshold_factor);

void updateBreathing(float x, float y, float z);

int getMinuteStepcount();

void resetMinuteStepcount();

float getUpperThreshold();

float getLowerThreshold();

float getBreathingSignal();

float getBreathingAngle();

float getBreathingRate();

void resetBreathingRate();

float getAverageBreathingRate();

float getStdDevBreathingRate();

void resetMedianAverageBreathing();

void calculateAverageBreathing();

int getNumberOfBreaths();

float getActivityLevel();

void updateActivityClassification();

int getCurrentActivityClassification();

#endif
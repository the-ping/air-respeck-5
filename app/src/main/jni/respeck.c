
#include "breathing/breathing.h"
#include "breathing/breath_detection.h"
#include "breathing/breathing_rate_stats.h"
#include "activityclassification/predictions.h"
#include "stepcount/step_count.h"

static BreathingMeasures breathing_buffer;
static ThresholdBuffer threshold_buffer;
static CurrentBreath current_breath;
static StepCounter step_counter;
static BreathingRateStats breathing_rate_stats;
static float upper_threshold;
static float lower_threshold;
static float th_factor = 1.f;

// Activity classification
static int current_activity_classification = -1;


char *testEcho(char* testString) {
    return testString;
}

void initBreathing(bool is_post_filtering_enabled, float activity_cutoff, unsigned int threshold_filter_size,
                   float lower_threshold_limit, float upper_threshold_limit, float threshold_factor) {
    initialise_breathing_measures(&breathing_buffer, is_post_filtering_enabled, activity_cutoff);
    initialise_rms_threshold_buffer(&threshold_buffer, threshold_filter_size);
    initialise_breath(&current_breath, lower_threshold_limit, upper_threshold_limit);
    initialise_breathing_rate_stats(&breathing_rate_stats);
    initialise_stepcounter(&step_counter);

    th_factor = threshold_factor;
    breathing_buffer.is_breathing_initialised = true;
}


void updateBreathing(float x, float y, float z) {
    float new_accel_data[3] = {x, y, z};

    // Update the step counter
    update_stepcounter(new_accel_data, &step_counter);

    update_breathing_measures(new_accel_data, &breathing_buffer);
    update_rms_threshold(breathing_buffer.signal, &threshold_buffer);

    // Adjust the rms threshold by some factor. TODO: determine best factor
    upper_threshold = threshold_buffer.upper_threshold_value / th_factor;
    lower_threshold = threshold_buffer.lower_threshold_value / th_factor;
    update_breath(breathing_buffer.signal, upper_threshold, lower_threshold, &current_breath);

    // If the breathing rate has been updated, add it to the statistics
    if (current_breath.is_complete && !isnan(current_breath.breathing_rate)) {
        update_breathing_rate_stats(current_breath.breathing_rate, &breathing_rate_stats);
        current_breath.is_complete = false;
    }
}

int getMinuteStepcount() {
    return step_counter.minute_step_count;
}

void resetMinuteStepcount() {
    step_counter.minute_step_count = 0;
}

float getUpperThreshold() {
    return upper_threshold;
}

float getLowerThreshold() {
    return lower_threshold;
}

float getBreathingSignal() {
    return breathing_buffer.signal;
}

float getBreathingAngle() {
    return breathing_buffer.angle;
}

float getBreathingRate() {
    return current_breath.breathing_rate;
}

float getAverageBreathingRate() {
    return breathing_rate_mean(&breathing_rate_stats);
}

float getStdDevBreathingRate() {
    return breathing_rate_standard_deviation(&breathing_rate_stats);
}

void resetMedianAverageBreathing() {
    initialise_breathing_rate_stats(&breathing_rate_stats);
}

void calculateAverageBreathing() {
    calculate_breathing_rate_stats(&breathing_rate_stats);
}

int getNumberOfBreaths() {
    return breathing_rate_number_of_breaths(&breathing_rate_stats);
}

float getActivityLevel() {
    return breathing_buffer.max_act_level;
}

void updateActivityClassification() {
    // Only do something if buffer is filled
    if (get_is_buffer_full()) {
        current_activity_classification = simple_predict();
    }
}

int getCurrentActivityClassification() {
    return current_activity_classification;
}

#include "breathing/breathing.h"
#include "breathing/breath_detection.h"
#include "breathing/breathing_rate_stats.h"
#include "activityclassification/predictions.h"
#include "stepcount/step_count.h"

static BreathingMeasures breathing_measures;
static ThresholdBuffer threshold_buffer;
static CurrentBreath current_breath;
static StepCounter step_counter;
static BreathingRateStats breathing_rate_stats;
static ActivityPredictor activity_predictor;

static float upper_threshold;
static float lower_threshold;
static float th_factor = 1.f;
static bool is_breath_end = false;

char *testEcho(char *testString) {
    return testString;
}

void initBreathing(int pre_filter_length, int post_filter_length, float activity_cutoff,
                   unsigned int threshold_filter_size,
                   float lower_threshold_limit, float upper_threshold_limit, float threshold_factor,
                   float sampling_frequency) {
    initialise_breathing_measures(&breathing_measures, (unsigned int) pre_filter_length,
                                  (unsigned int) post_filter_length, activity_cutoff);
    initialise_rms_threshold_buffer(&threshold_buffer, threshold_filter_size);
    initialise_breath(&current_breath, lower_threshold_limit, upper_threshold_limit, sampling_frequency);
    initialise_breathing_rate_stats(&breathing_rate_stats);
    initialise_stepcounter(&step_counter);
    initialise_activity_classification(&activity_predictor);

    th_factor = threshold_factor;
    breathing_measures.is_breathing_initialised = true;
}


void updateBreathing(float x, float y, float z) {
    float new_accel_data[3] = {x, y, z};

    // Update the step counter
    update_stepcounter(new_accel_data, &step_counter);

    update_breathing_measures(new_accel_data, &breathing_measures, &step_counter, &activity_predictor);
    update_rms_threshold(breathing_measures.signal, &threshold_buffer);

    // Adjust the rms threshold by some factor.
    upper_threshold = threshold_buffer.upper_threshold_value / th_factor;
    lower_threshold = threshold_buffer.lower_threshold_value / th_factor;
    update_breath(breathing_measures.signal, upper_threshold, lower_threshold, &current_breath);

    // If the breathing rate has been updated, add it to the statistics
    if (current_breath.is_complete && !isnan(current_breath.breathing_rate)) {
        update_breathing_rate_stats(current_breath.breathing_rate, &breathing_rate_stats);
        current_breath.is_complete= false;
        is_breath_end = true;
    } else {
        is_breath_end = false;
    }
}

// This method should be used when using the library to calculate the breathing rate with a given breathing signal,
// but without the acceleration data. The input could be the breathing signal from a nasal cannula or pressure
// belt, for instance.
void updateBreathingSignal(float breathingSignal) {
    update_rms_threshold(breathingSignal, &threshold_buffer);

    // Adjust the rms threshold by some factor which was set during initialisation of the library.
    upper_threshold = threshold_buffer.upper_threshold_value / th_factor;
    lower_threshold = threshold_buffer.lower_threshold_value / th_factor;
    update_breath(breathingSignal, upper_threshold, lower_threshold, &current_breath);

    // If the breathing rate has been updated, add it to the statistics
    if (current_breath.is_complete && !isnan(current_breath.breathing_rate)) {
        update_breathing_rate_stats(current_breath.breathing_rate, &breathing_rate_stats);
        current_breath.is_complete = false;
        is_breath_end = true;
    } else {
        is_breath_end = false;
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
    return breathing_measures.signal;
}

float getBreathingAngle() {
    return breathing_measures.angle;
}

float getBreathingRate() {
    return current_breath.breathing_rate;
}

void resetBreathingRate() {
    // This is used in the Western General analysis to reset the breathing rate to NaN once
    // a new breathing rate has been read. This makes it possible to clearly tell when breaths have ended,
    // as otherwise two consecutive breaths with exactly the same breathing rate will be indistinguishable
    current_breath.breathing_rate = NAN;
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
    return breathing_measures.max_act_level;
}

int getActivityClassification() {
    return activity_predictor.last_prediction;
}

bool getIsBreathEnd() {
    return is_breath_end;
}

void updateSamplingFrequency(float sampling_frequency) {
    update_sampling_frequency(sampling_frequency, &current_breath);
}
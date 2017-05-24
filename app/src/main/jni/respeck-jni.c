/**
 *
 */

#include <jni.h>

#include "breathing/breathing.h"
#include "breathing/breath_detection.h"
#include "breathing/breathing_rate_stats.h"
#include "activityclassification/predictions.h"

static BreathingMeasures breathing_buffer;
static ThresholdBuffer threshold_buffer;
static CurrentBreath current_breath;
static BreathingRateStats breathing_rate_stats;
static float upper_threshold;
static float lower_threshold;

// Activity classification
static int current_activity_classification = -1;

JNIEXPORT jstring JNICALL
Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getMsgFromJni(JNIEnv *env, jobject instance) {
    return (*env)->NewStringUTF(env, "It works");
}

void Java_com_specknet_airrespeck_services_RESpeckPacketHandler_initBreathing(JNIEnv *env, jobject this,
                                                                               bool isPostFilteringEnabled) {
    initialise_breathing_measures(&breathing_buffer, isPostFilteringEnabled);
    initialise_rms_threshold_buffer(&threshold_buffer);
    initialise_breath(&current_breath);
    initialise_breathing_rate_stats(&breathing_rate_stats);

    breathing_buffer.is_breathing_initialised = true;
}


void Java_com_specknet_airrespeck_services_RESpeckPacketHandler_updateBreathing(JNIEnv *env, jobject this, float x,
                                                                                 float y, float z) {
    float new_accel_data[3] = {x, y, z};
    update_breathing_measures(new_accel_data, &breathing_buffer);
    update_rms_threshold(breathing_buffer.signal, &threshold_buffer);

    // Adjust the rms threshold by some factor. TODO: determine best factor
    upper_threshold = threshold_buffer.upper_threshold_value / 3.f;
    lower_threshold = threshold_buffer.lower_threshold_value / 3.f;
    update_breath(breathing_buffer.signal, upper_threshold, lower_threshold, &current_breath);

    // If the breathing rate has been updated, add it to the
    if (current_breath.is_complete && !isnan(current_breath.breathing_rate)) {
        update_breathing_rate_stats(current_breath.breathing_rate, &breathing_rate_stats);
        current_breath.is_complete = false;
    }
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getUpperThreshold(JNIEnv *env, jobject this) {
    return upper_threshold;
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getLowerThreshold(JNIEnv *env, jobject this) {
    return lower_threshold;
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getBreathingSignal(JNIEnv *env, jobject this) {
    return (jfloat) breathing_buffer.signal;
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getBreathingAngle(JNIEnv *env, jobject this) {
    return (jfloat) breathing_buffer.angle;
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getBreathingRate(JNIEnv *env, jobject this) {
    return (jfloat) current_breath.breathing_rate;
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getAverageBreathingRate(JNIEnv *env, jobject this) {
    return (jfloat) breathing_rate_mean(&breathing_rate_stats);
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getStdDevBreathingRate(JNIEnv *env, jobject this) {
    return (jfloat) breathing_rate_standard_deviation(&breathing_rate_stats);
}

void Java_com_specknet_airrespeck_services_RESpeckPacketHandler_resetMedianAverageBreathing(JNIEnv *env,
                                                                                             jobject this) {
    initialise_breathing_rate_stats(&breathing_rate_stats);
}

void Java_com_specknet_airrespeck_services_RESpeckPacketHandler_calculateAverageBreathing(JNIEnv *env,
                                                                                           jobject this) {
    calculate_breathing_rate_stats(&breathing_rate_stats);
}

jint Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getNumberOfBreaths(JNIEnv *env, jobject this) {
    return (jint) breathing_rate_number_of_breaths(&breathing_rate_stats);
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getActivityLevel(JNIEnv *env, jobject this) {
    return (jfloat) breathing_buffer.max_act_level;
}

void Java_com_specknet_airrespeck_services_RESpeckPacketHandler_updateActivityClassification(JNIEnv *env,
                                                                                              jobject instance) {
    // Only do something if buffer is filled
    if (get_is_buffer_full()) {
        current_activity_classification = simple_predict();
    }
}

jint Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getCurrentActivityClassification(JNIEnv *env,
                                                                                                  jobject instance) {
    return (jint) current_activity_classification;
}
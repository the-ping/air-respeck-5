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
static BreathingRateBuffer breathing_rate_buffer;
static float upper_threshold;
static float lower_threshold;

// Activity classification
static int current_activity_classification = -1;

JNIEXPORT jstring JNICALL
Java_com_specknet_airrespeck_services_SpeckBluetoothService_getMsgFromJni(JNIEnv *env, jobject instance) {
    return (*env)->NewStringUTF(env, "It works");
}

void Java_com_specknet_airrespeck_services_SpeckBluetoothService_initBreathing(JNIEnv *env, jobject this) {
    initialise_breathing_measures(&breathing_buffer);
    initialise_rms_threshold_buffer(&threshold_buffer);
    initialise_breath(&current_breath);
    initialise_breathing_rate_buffer(&breathing_rate_buffer);

    breathing_buffer.is_breathing_initialised = true;
}


void Java_com_specknet_airrespeck_services_SpeckBluetoothService_updateBreathing(JNIEnv *env, jobject this, float x,
                                                                                 float y, float z) {
    float new_accel_data[3] = {x, y, z};
    update_breathing_measures(new_accel_data, &breathing_buffer);
    update_rms_threshold(breathing_buffer.signal, &threshold_buffer);

    // Adjust the rms threshold by some factor which was determined empirically on the Western General data
    upper_threshold = threshold_buffer.upper_threshold_value / 4.f;
    lower_threshold = threshold_buffer.lower_threshold_value / 4.f;
    update_breath(breathing_buffer.signal, upper_threshold, lower_threshold, &current_breath);

    // If the breathing rate has been updated, add it to the
    if (current_breath.is_complete && !isnan(current_breath.breathing_rate)) {
        update_breathing_rate_buffer(current_breath.breathing_rate, &breathing_rate_buffer);
        current_breath.is_complete = false;
    }
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getUpperThreshold(JNIEnv *env, jobject this) {
    return upper_threshold;
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getLowerThreshold(JNIEnv *env, jobject this) {
    return lower_threshold;
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getBreathingSignal(JNIEnv *env, jobject this) {
    return (jfloat) breathing_buffer.signal;
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getBreathingAngle(JNIEnv *env, jobject this) {
    return (jfloat) breathing_buffer.angle;
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getBreathingRate(JNIEnv *env, jobject this) {
    return (jfloat) current_breath.breathing_rate;
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getAverageBreathingRate(JNIEnv *env, jobject this) {
    return (jfloat) breathing_rate_mean(&breathing_rate_buffer);
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getStdDevBreathingRate(JNIEnv *env, jobject this) {
    return (jfloat) breathing_rate_standard_deviation(&breathing_rate_buffer);
}

void Java_com_specknet_airrespeck_services_SpeckBluetoothService_resetMedianAverageBreathing(JNIEnv *env,
                                                                                             jobject this) {
    initialise_breathing_rate_buffer(&breathing_rate_buffer);
}

void Java_com_specknet_airrespeck_services_SpeckBluetoothService_calculateMedianAverageBreathing(JNIEnv *env,
                                                                                                 jobject this) {
    calculate_breathing_rate_stats(&breathing_rate_buffer);
}

jint Java_com_specknet_airrespeck_services_SpeckBluetoothService_getNumberOfBreaths(JNIEnv *env, jobject this) {
    return (jint) breathing_rate_number_of_breaths(&breathing_rate_buffer);
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getActivityLevel(JNIEnv *env, jobject this) {
    return (jfloat) breathing_buffer.max_act_level;
}

void Java_com_specknet_airrespeck_services_SpeckBluetoothService_updateActivityClassification(JNIEnv *env,
                                                                                              jobject instance) {
    // Only do something if buffer is filled
    if (get_is_buffer_full()) {
        current_activity_classification = simple_predict();
    }
}

jint Java_com_specknet_airrespeck_services_SpeckBluetoothService_getCurrentActivityClassification(JNIEnv *env,
                                                                                                  jobject instance) {
    return (jint) current_activity_classification;
}
#include <jni.h>

#include "respeck.h"

JNIEXPORT jstring JNICALL
Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getMsgFromJni(JNIEnv *env, jobject instance) {
    return (*env)->NewStringUTF(env, "It works");
}

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
JNIEXPORT void JNICALL
Java_com_specknet_airrespeck_services_RESpeckPacketHandler_initBreathing(JNIEnv *env, jobject instance,
                                                                         jboolean is_post_filtering_enabled,
                                                                         jfloat activity_cutoff,
                                                                         jint threshold_filter_size,
                                                                         jfloat lower_threshold_limit,
                                                                         jfloat upper_threshold_limit,
                                                                         jfloat threshold_factor,
                                                                         jfloat sampling_frequency) {

    if (is_post_filtering_enabled) {
        initBreathing(12, 12, activity_cutoff, threshold_filter_size, lower_threshold_limit,
                      upper_threshold_limit, threshold_factor, sampling_frequency);
    } else {
        initBreathing(12, 1, activity_cutoff, threshold_filter_size, lower_threshold_limit,
                      upper_threshold_limit, threshold_factor, sampling_frequency);
    }

}

void Java_com_specknet_airrespeck_services_RESpeckPacketHandler_resetMinuteStepcount(JNIEnv *env, jobject this) {
    resetMinuteStepcount();
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getUpperThreshold(JNIEnv *env, jobject this) {
    return getUpperThreshold();
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getLowerThreshold(JNIEnv *env, jobject this) {
    return getLowerThreshold();
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getBreathingSignal(JNIEnv *env, jobject this) {
    return getBreathingSignal();
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getBreathingAngle(JNIEnv *env, jobject this) {
    return getBreathingAngle();
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getBreathingRate(JNIEnv *env, jobject this) {
    return getBreathingRate();
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getAverageBreathingRate(JNIEnv *env, jobject this) {
    return getAverageBreathingRate();
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getStdDevBreathingRate(JNIEnv *env, jobject this) {
    return getStdDevBreathingRate();
}

void Java_com_specknet_airrespeck_services_RESpeckPacketHandler_resetMedianAverageBreathing(JNIEnv *env,
                                                                                            jobject this) {
    resetMedianAverageBreathing();
}

void Java_com_specknet_airrespeck_services_RESpeckPacketHandler_calculateAverageBreathing(JNIEnv *env,
                                                                                          jobject this) {
    calculateAverageBreathing();
}

jint Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getNumberOfBreaths(JNIEnv *env, jobject this) {
    return getNumberOfBreaths();
}

jfloat Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getActivityLevel(JNIEnv *env, jobject this) {
    return getActivityLevel();
}

jint Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getActivityClassification(JNIEnv *env,
                                                                                          jobject instance) {
    return getActivityClassification();
}

jboolean Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getIsBreathEnd(JNIEnv *env, jobject instance) {
    return (jboolean) getIsBreathEnd();
}

void Java_com_specknet_airrespeck_services_RESpeckPacketHandler_resetBreathingRate(JNIEnv *env, jobject instance) {
    resetBreathingRate();
}

JNIEXPORT void JNICALL
Java_com_specknet_airrespeck_services_RESpeckPacketHandler_updateBreathing(JNIEnv *env, jobject instance, jfloat x,
                                                                           jfloat y, jfloat z) {

    updateBreathing(x, y, z);

}

JNIEXPORT void JNICALL
Java_com_specknet_airrespeck_services_RESpeckPacketHandler_updateSamplingFrequency(JNIEnv *env, jobject instance, jfloat sampling_frequency) {

    updateSamplingFrequency(sampling_frequency);

}

JNIEXPORT jint JNICALL
Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getMinuteStepcount(JNIEnv *env, jobject instance) {

    return getMinuteStepcount();

}


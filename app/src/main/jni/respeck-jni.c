/**
 *
 */

#include <jni.h>
#include <stdbool.h>

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
void Java_com_specknet_airrespeck_services_RESpeckPacketHandler_initBreathing(JNIEnv *env, jobject this,
                                                                              bool is_post_filtering_enabled,
                                                                              float activity_cutoff,
                                                                              unsigned int threshold_filter_size,
                                                                              float lower_threshold_limit,
                                                                              float upper_threshold_limit,
                                                                              float threshold_factor) {
    initBreathing(is_post_filtering_enabled, activity_cutoff, threshold_filter_size, lower_threshold_limit,
                  upper_threshold_limit, threshold_factor);
}


void Java_com_specknet_airrespeck_services_RESpeckPacketHandler_updateBreathing(JNIEnv *env, jobject this, float x,
                                                                                float y, float z) {
    updateBreathing(x, y, z);
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

void Java_com_specknet_airrespeck_services_RESpeckPacketHandler_updateActivityClassification(JNIEnv *env,
                                                                                             jobject instance) {
    updateActivityClassification();
}

jint Java_com_specknet_airrespeck_services_RESpeckPacketHandler_getCurrentActivityClassification(JNIEnv *env,
                                                                                                 jobject instance) {
    return getCurrentActivityClassification();
}
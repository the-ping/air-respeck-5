//
// Created by Darius on 09.02.2017.
//

#include <string.h>
#include <jni.h>

#include "breathing/breathing.h"
#include "breathing/breathing_rate.h"
#include "breathing/ma_stats.h"
#include "activityclassification/predictions.h"

static breathing_filter breathing;
static threshold_filter thresholds;
static bpm_filter bpm;
static ma_stats_filter maf;
static int count;

// Activity classification
static int current_activity_classification = -1;

JNIEXPORT jstring JNICALL
                  Java_com_specknet_airrespeck_services_SpeckBluetoothService_getMsgFromJni(JNIEnv *env, jobject instance) {
    return (*env)->NewStringUTF(env, "It works");
}

void Java_com_specknet_airrespeck_services_SpeckBluetoothService_initBreathing( JNIEnv* env, jobject this )
{
    BRG_init(&breathing);
    threshold_init(&thresholds);
    bpm_init(&bpm);
    MA_stats_init(&maf);

    breathing.sample_rate = SAMPLE_RATE;
    breathing.sample_rate_valid = true;
    count = 0;
}


void Java_com_specknet_airrespeck_services_SpeckBluetoothService_updateBreathing(JNIEnv *env, jobject this, float x, float y, float z)
{
    double accel[3] = {x,y,z};
    BRG_update(accel, &breathing);
    update_threshold(breathing.bs, &thresholds);

    float ut = thresholds.upper_value / 2.f;
    float lt = thresholds.lower_value / 2.f;
    bpm_update(breathing.bs, ut, lt, &bpm);
    count++;

    if (bpm.updated && !isnan(bpm.bpm)) {
        count = 0;
        MA_stats_update(bpm.bpm, &maf);
        bpm.updated = false;
    }
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getBreathingSignal( JNIEnv* env, jobject this)
{
    return (jfloat) breathing.bs;
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getBreathingAngle( JNIEnv* env, jobject this)
{
    return (jfloat) breathing.ba;
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getBreathingRate( JNIEnv* env, jobject this)
{
    return (jfloat) bpm.bpm;
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getAverageBreathingRate( JNIEnv* env, jobject this)
{
    return (jfloat) MA_stats_mean(&maf);
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getStdDevBreathingRate( JNIEnv* env, jobject this)
{
    return (jfloat) MA_stats_sd(&maf);
}

void Java_com_specknet_airrespeck_services_SpeckBluetoothService_resetMedianAverageBreathing( JNIEnv* env, jobject this)
{
    MA_stats_init(&maf);
}

void Java_com_specknet_airrespeck_services_SpeckBluetoothService_calculateMedianAverageBreathing( JNIEnv* env, jobject this)
{
    MA_stats_calculate(&maf);
}

jint Java_com_specknet_airrespeck_services_SpeckBluetoothService_getNumberOfBreaths( JNIEnv* env, jobject this)
{
    return (jint) MA_stats_num(&maf);
}

jfloat Java_com_specknet_airrespeck_services_SpeckBluetoothService_getActivityLevel( JNIEnv* env, jobject this)
{
    return (jfloat) breathing.activity;
}

void Java_com_specknet_airrespeck_services_SpeckBluetoothService_updateActivityClassification(JNIEnv *env, jobject instance) {
    // Only do something if buffer is filled
    if (get_is_buffer_full()) {
        current_activity_classification = simple_predict();
    }
}

jint Java_com_specknet_airrespeck_services_SpeckBluetoothService_getCurrentActivityClassification(JNIEnv *env, jobject instance) {
    return (jint) current_activity_classification;
}
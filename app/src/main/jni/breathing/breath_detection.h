//
//  breathing.h
//  TemperatureSensor
//
//  Created by andy on 16/03/2013.
//  Copyright (c) 2013 Apple Inc. All rights reserved.
//

#ifndef TemperatureSensor_breathing_h
#define TemperatureSensor_breathing_h

#include <stdio.h>
#include <stdbool.h>
#include <math.h>

#define HIGHEST_POSSIBLE_BREATHING_RATE 45
#define LOWEST_POSSIBLE_BREATHING_RATE 5

typedef enum {
    POSITIVE,
    INVALID,
    NEGATIVE
} threshold_value_type;

typedef enum {
    LOW,
    MID_FALLING,
    MID_UNKNOWN,
    MID_RISING,
    HIGH,
    UNKNOWN
} bpm_state;


typedef struct {
    int current_position, fill;
    float *values;
    threshold_value_type *values_type;
    int upper_values_sum_fill, lower_values_sum_fill;
    float upper_values_sum, lower_values_sum;
    int threshold_filter_size;

    bool is_valid;
    float upper_threshold_value, lower_threshold_value;
} ThresholdBuffer;


typedef struct {
    bpm_state state;
    bool valid;
    float breathing_rate;
    float min_threshold;
    float max_threshold;
    int sample_count;
    bool is_current_breath_valid;
    bool is_complete;
} CurrentBreath;


void initialise_rms_threshold_buffer(ThresholdBuffer *threshold_buffer, unsigned int threshold_filter_size);

void update_rms_threshold(float breathing_signal_value, ThresholdBuffer *threshold_buffer);

void initialise_breath(CurrentBreath *breath);

void update_breath(float breathing_signal, float upper_threshold, float lower_threshold,
                   CurrentBreath *breath);

#endif

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

#define THRESHOLD_FILTER_SIZE (12*10)

typedef enum
{
	POSITIVE,
	INVALID,
	NEGATIVE
} threshold_value_type;

typedef enum
{
    
	LOW,
	MID_FALLING,
	MID_UNKNOWN,
	MID_RISING,
	HIGH,
	UNKNOWN
    
} bpm_state;



typedef struct
{
	int pos, fill;
	float values[THRESHOLD_FILTER_SIZE];
	threshold_value_type values_type[THRESHOLD_FILTER_SIZE];
	int upper_value_sum_fill, lower_value_sum_fill;
	float upper_value_sum, lower_value_sum;
    
	bool valid;
	float upper_value, lower_value;
    
} threshold_filter;


typedef struct
{
	bpm_state state;
	bool valid;
    float bpm;
    float min_threshold;
    float max_threshold;
    int sample_count;
    bool sample_count_valid;
    float sample_rate;
    bool updated;

	   
} bpm_filter;


void threshold_init(threshold_filter *filter);
void update_threshold(float value, threshold_filter *filter);

void bpm_init(bpm_filter *filter);
void bpm_update(float breathing_value, float upper_threshold, float lower_threshold, bpm_filter *filter);

#endif

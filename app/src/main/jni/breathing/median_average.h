//
//  median_average.h
//  TemperatureSensor
//
//  Created by andy on 26/04/2013.
//  Copyright (c) 2013 Apple Inc. All rights reserved.
//

#ifndef TemperatureSensor_median_average_h
#define TemperatureSensor_median_average_h

#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <time.h>

#define AVERAGE_SIZE 14
#define UPPER 2
#define LOWER 2
#define INVALIDATION_TIME_SECS 60

typedef struct
{
	uint8_t pos;
	uint8_t fill;
    
	float values[AVERAGE_SIZE];
    float sorted[AVERAGE_SIZE];
	float n;
	float sd;
	float value;
	bool valid;
	time_t timestamp;
    
} median_average_filter;

void MAVG_init(median_average_filter* filter);
void MAVG_update(float value, median_average_filter* filter);
int compare_function(const void *a,const void *b);
bool MAVG_check_valid(median_average_filter* filter);

#endif

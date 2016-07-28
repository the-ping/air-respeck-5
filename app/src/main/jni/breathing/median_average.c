//
//  median_average.c
//  TemperatureSensor
//
//  Created by andy on 26/04/2013.
//  Copyright (c) 2013 Apple Inc. All rights reserved.
//
#include <math.h>
#include "median_average.h"

void MAVG_init(median_average_filter* filter)
{
	filter->pos = 0;
	filter->fill = 0;
	filter->valid = false;
	filter->timestamp = time(NULL);
	filter->value = NAN;
	filter->sd = NAN;
	filter->n = 0;
    
	uint8_t i;
	for (i = 0; i < AVERAGE_SIZE; i++)
	{
		filter->values[i] = 0;
	}
}

int compare_function(const void *a,const void *b) {
    float *x = (float *) a;
    float *y = (float *) b;
    // return *x - *y; // this is WRONG...
    if (*x < *y) return -1;
    else if (*x > *y) return 1; return 0;
}

void MAVG_update(float value, median_average_filter* filter)
{
	filter->values[filter->pos] = value;
    
	filter->pos = (filter->pos + 1) % AVERAGE_SIZE;
    
	if (filter->fill < AVERAGE_SIZE)
	{
		filter->fill++;
	}
    
	if (filter->fill < AVERAGE_SIZE)
	{
		filter->valid = false;
		return;
	}
    
    // copy to sorted array
    int i;
    for (i=0;i < AVERAGE_SIZE; i++) {
        filter->sorted[i] = filter->values[i];
    }
    
    qsort(filter->sorted, AVERAGE_SIZE, sizeof(value), compare_function);
    
    // calculate mean of central part of sorted array
    float sum = 0;
    int count = 0;
    int j;
    for (j = LOWER; j < AVERAGE_SIZE - UPPER; j++) {
        sum += filter->sorted[j];
        count++;
    }
    
	filter->value = sum / (float)count;
	filter->valid = true;
	filter->n = count;
	filter->sd = 0;
	filter->timestamp = time(NULL);
    
}

bool MAVG_check_valid(median_average_filter* filter) {
	time_t now = time(NULL);
	double diff_secs = difftime(now,filter->timestamp);
	if (diff_secs > INVALIDATION_TIME_SECS) {
		filter->valid = false;
		filter->n = 0;
		filter->sd = NAN;
		filter->value = NAN;

		return false;
	}
	return true;
}
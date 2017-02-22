//
//  median_average.h
//  TemperatureSensor
//
//  Created by andy on 26/04/2013.
//  Copyright (c) 2013 Apple Inc. All rights reserved.
//

#ifndef __ma_stats_h
#define __ma_stats_h

#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>

#define MA_STATS_AVERAGE_SIZE 32
#define MA_STATS_UPPER 2
#define MA_STATS_LOWER 2

typedef struct {
    uint8_t fill;

    float values[MA_STATS_AVERAGE_SIZE];
    bool valid;
    float oldM;
    float newM;
    float oldS;
    float newS;
    float max;
    float min;
} ma_stats_filter;

void MA_stats_init(ma_stats_filter *filter);

void MA_stats_update(float value, ma_stats_filter *filter);

void MA_stats_calculate(ma_stats_filter *filter);

int MA_stats_num(ma_stats_filter *filter);

float MA_stats_mean(ma_stats_filter *filter);

float MA_stats_var(ma_stats_filter *filter);

float MA_stats_sd(ma_stats_filter *filter);

#endif // __ma_stats_h

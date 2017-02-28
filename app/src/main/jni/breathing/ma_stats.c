//
//  median_average.c
//  TemperatureSensor
//
//  Created by andy on 26/04/2013.
//  Copyright (c) 2013 Apple Inc. All rights reserved.
//
#include "ma_stats.h"
#include <math.h>

void MA_stats_init(ma_stats_filter *filter) {
    filter->fill = 0;
    filter->valid = false;
}

int ma_compare_function(const void *a, const void *b) {
    float *x = (float *) a;
    float *y = (float *) b;
    // return *x - *y; // this is WRONG...
    if (*x < *y) {
        return -1;
    } else if (*x > *y) {
        return 1;
    }
    return 0;
}

void MA_stats_update(float value, ma_stats_filter *filter) {
    if (filter->fill < MA_STATS_AVERAGE_SIZE) {
        filter->values[filter->fill] = value;
        filter->fill++;
    }
}

void MA_stats_calculate(ma_stats_filter *filter) {
    float value;
    // Sort the values in the minute array (filter)
    qsort(filter->values, filter->fill, sizeof(filter->values[0]), ma_compare_function);


    value = filter->values[MA_STATS_LOWER];
    filter->oldM = filter->newM = value;
    filter->oldS = 0.0;
    filter->max = value;
    filter->min = value;

    int i;
    for (i = MA_STATS_LOWER + 1; i < filter->fill - MA_STATS_UPPER; i++) {
        value = filter->values[i];
        filter->newM = filter->oldM + (value - filter->oldM) / filter->fill;
        filter->newS = filter->oldS + (value - filter->oldM) * (value - filter->newM);

        filter->oldM = filter->newM;
        filter->oldS = filter->newS;

        filter->max = (value > filter->max) ? value : filter->max;
        filter->min = (value < filter->min) ? value : filter->min;
        filter->valid = true;
    }
}

int MA_stats_num(ma_stats_filter *filter) {
    return filter->fill;
}

float MA_stats_mean(ma_stats_filter *filter) {
    return (filter->fill > 0) ? filter->newM : NAN;
}

float MA_stats_var(ma_stats_filter *filter) {
    return ((filter->fill > 1) ? filter->newS / (filter->fill - 1) : 0.0f);
}

float MA_stats_sd(ma_stats_filter *filter) {
    return sqrt(MA_stats_var(filter));
}

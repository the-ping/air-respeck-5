#ifndef __MEAN_BUFFER_3D_H__
#define __MEAN_BUFFER_3D_H__

#include <stdbool.h>
#include <stdint.h>

#define MEAN_ACCEL_FILTER_SIZE 12

typedef struct {

    float sum[3];
    int current_position;
    int fill;

    float values[MEAN_ACCEL_FILTER_SIZE][3];
    float mean_accel_values[3];
    bool is_valid;

} MeanAccelFilter;

void initialise_mean_accel_filter(MeanAccelFilter *mean_accel_filter);

void update_mean_accel_filter(float *new_accel_data, MeanAccelFilter *mean_accel_filter);

#endif

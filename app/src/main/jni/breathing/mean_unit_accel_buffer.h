#ifndef __MEAN_ACCEL_BUFFER_H__
#define __MEAN_ACCEL_BUFFER_H__

#include <stdbool.h>
#include <stdint.h>

#define MEAN_ACCEL_BUFFER_SIZE 128

typedef struct {

    double sum[3];
    int current_position;
    int fill;

    double values[MEAN_ACCEL_BUFFER_SIZE][3];
    double mean_unit_vector[3];
    bool is_valid;

} MeanUnitAccelBuffer;

void initialise_mean_unit_accel_buffer(MeanUnitAccelBuffer *mean_accel_buffer);

void update_mean_unit_accel_buffer(double *new_accel_data, MeanUnitAccelBuffer *mean_accel_buffer);

#endif

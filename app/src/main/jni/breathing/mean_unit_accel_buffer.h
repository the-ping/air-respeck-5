#ifndef __MEAN_ACCEL_BUFFER_H__
#define __MEAN_ACCEL_BUFFER_H__

#include <stdbool.h>
#include <stdint.h>

typedef struct {
    float sum[3];
    int current_position;
    int fill;
    unsigned int buffer_size;

    float mean_unit_vector[3];
    bool is_valid;
    float **values;
} MeanUnitAccelBuffer;

void initialise_mean_unit_accel_buffer(MeanUnitAccelBuffer *mean_accel_buffer, unsigned int buffer_size);

void update_mean_unit_accel_buffer(float *new_accel_data, MeanUnitAccelBuffer *mean_accel_buffer);

#endif

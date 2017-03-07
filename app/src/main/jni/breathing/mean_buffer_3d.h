#ifndef __MEAN_BUFFER_3D_H__
#define __MEAN_BUFFER_3D_H__

#include <stdbool.h>
#include <stdint.h>

#define MEAN_AVERAGE_SIZE 12

typedef struct {

    double sum[3];
    int current_position;
    int fill;

    double values[MEAN_AVERAGE_SIZE][3];
    double mean_values[3];
    bool is_valid;

} MeanBuffer3D;

void initialise_mean_buffer_3d(MeanBuffer3D *mean_buffer_3d);

void update_mean_buffer_3d(double *new_accel_data, MeanBuffer3D *mean_buffer_3d);

#endif

#ifndef __AXIS_AND_ANGLE_H__
#define __AXIS_AND_ANGLE_H__

#include <stdbool.h>
#include "math_helper.h"

typedef struct {

    double previous_accel_data[3];
    bool is_previous_accel_data_valid;

    double current_axis[3];
    bool is_current_axis_valid;

} RotationAxisBuffer;

void initialise_rotation_axis_buffer(RotationAxisBuffer *axis_and_angle_buffer);

void update_rotation_axis_buffer(double *new_accel_data, RotationAxisBuffer *rotation_axis_buffer);

#endif

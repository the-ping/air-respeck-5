#ifndef __AXIS_AND_ANGLE_H__
#define __AXIS_AND_ANGLE_H__

#include <stdbool.h>
#include "math_helper.h"

typedef struct {

    float previous_accel_data[3];
    bool is_previous_accel_data_valid;

    float current_axis[3];
    bool is_current_axis_valid;

} RotationAxis;

void initialise_rotation_axis(RotationAxis *axis_and_angle_buffer);

void update_rotation_axis(float *new_accel_data, RotationAxis *rotation_axis_buffer);

#endif

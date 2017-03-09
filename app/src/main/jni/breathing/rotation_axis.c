/**
 * This class currently computes the axis on which consecutive acceleration vectors are turning. No angle is
 * computed, despite the name. TODO: change name if no angles are used!
 */
#include "rotation_axis.h"

void initialise_rotation_axis_buffer(RotationAxisBuffer *axis_and_angle_buffer) {
    axis_and_angle_buffer->is_current_axis_valid = false;
    axis_and_angle_buffer->is_previous_accel_data_valid = false;
}

void update_rotation_axis_buffer(float *new_accel_data, RotationAxisBuffer *rotation_axis_buffer) {

    // If there is no previous acceleration data, store the current data and return
    if (rotation_axis_buffer->is_previous_accel_data_valid == false) {
        copy_accel_vector(rotation_axis_buffer->previous_accel_data, new_accel_data);

        rotation_axis_buffer->is_previous_accel_data_valid = true;
        rotation_axis_buffer->is_current_axis_valid = false;
        return;
    }

    // Store the cross product of the previous and new accel data as the "current axis".
    float cross_product_out[3];
    cross_product(cross_product_out, rotation_axis_buffer->previous_accel_data, new_accel_data);
    copy_accel_vector(rotation_axis_buffer->current_axis, cross_product_out);

    copy_accel_vector(rotation_axis_buffer->previous_accel_data, new_accel_data);
    rotation_axis_buffer->is_current_axis_valid = true;
}

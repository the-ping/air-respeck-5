#include "mean_axis_buffer.h"

#include "math_helper.h"
//#include "arm_math.h"

void get_reference_axis(float *ref);

void initialise_mean_rotation_axis_buffer(MeanRotationAxisBuffer *mean_axis_buffer) {

    mean_axis_buffer->fill = 0;
    mean_axis_buffer->current_position = -1;
    mean_axis_buffer->is_valid = 0;

    mean_axis_buffer->sum[0] = 0;
    mean_axis_buffer->sum[1] = 0;
    mean_axis_buffer->sum[2] = 0;

    for (int i = 0; i < MEAN_AXIS_SIZE; i++) {
        mean_axis_buffer->accel_buffer[i][0] = 0;
        mean_axis_buffer->accel_buffer[i][1] = 0;
        mean_axis_buffer->accel_buffer[i][2] = 0;
    }
}

void update_mean_rotation_axis_buffer(float *new_accel_data, MeanRotationAxisBuffer *mean_axis_buffer) {
    // Increment position
    mean_axis_buffer->current_position = (mean_axis_buffer->current_position + 1) % MEAN_AXIS_SIZE;

    float reference_axis[3], dot_result;
    get_reference_axis(reference_axis);

    subtract_from_accel_vector(mean_axis_buffer->sum,
                               mean_axis_buffer->accel_buffer[mean_axis_buffer->current_position]);

    copy_accel_vector(mean_axis_buffer->accel_buffer[mean_axis_buffer->current_position], new_accel_data);

    // Compare acceleration data to reference axis. If negative, invert acceleration data!
    dot_result = dot_product(new_accel_data, reference_axis);

    if (dot_result < 0) {
        mean_axis_buffer->accel_buffer[mean_axis_buffer->current_position][0] *= -1.0;
        mean_axis_buffer->accel_buffer[mean_axis_buffer->current_position][1] *= -1.0;
        mean_axis_buffer->accel_buffer[mean_axis_buffer->current_position][2] *= -1.0;
    }

    add_to_accel_vector(mean_axis_buffer->sum, mean_axis_buffer->accel_buffer[mean_axis_buffer->current_position]);

    if (mean_axis_buffer->fill < MEAN_AXIS_SIZE) {
        mean_axis_buffer->fill++;
    }

    if (mean_axis_buffer->fill < MEAN_AXIS_SIZE) {
        mean_axis_buffer->is_valid = false;
        return;
    }

    copy_accel_vector(mean_axis_buffer->mean_axis, mean_axis_buffer->sum);

    normalise_vector_to_unit_length(mean_axis_buffer->mean_axis);
    mean_axis_buffer->is_valid = true;
}

// TODO: why this reference axis and not another one?
void get_reference_axis(float *ref) {
    ref[0] = 0.98499424f;
    ref[1] = -0.17221591f;
    ref[2] = 0.01131468f;
}

#include "mean_unit_accel_buffer.h"
#include "math_helper.h"

void initialise_mean_unit_accel_buffer(MeanUnitAccelBuffer *mean_accel_buffer) {

    mean_accel_buffer->fill = 0;
    mean_accel_buffer->current_position = 0;
    mean_accel_buffer->is_valid = 0;

    mean_accel_buffer->sum[0] = 0;
    mean_accel_buffer->sum[1] = 0;
    mean_accel_buffer->sum[2] = 0;

    for (int i = 0; i < MEAN_ACCEL_BUFFER_SIZE; i++) {
        mean_accel_buffer->values[i][0] = 0;
        mean_accel_buffer->values[i][1] = 0;
        mean_accel_buffer->values[i][2] = 0;
    }
}

void update_mean_unit_accel_buffer(float *new_accel_data, MeanUnitAccelBuffer *mean_accel_buffer) {
    subtract_from_accel_vector(mean_accel_buffer->sum, mean_accel_buffer->values[mean_accel_buffer->current_position]);
    copy_accel_vector(mean_accel_buffer->values[mean_accel_buffer->current_position], new_accel_data);
    add_to_accel_vector(mean_accel_buffer->sum, mean_accel_buffer->values[mean_accel_buffer->current_position]);

    mean_accel_buffer->current_position = (mean_accel_buffer->current_position + 1) % MEAN_ACCEL_BUFFER_SIZE;

    if (mean_accel_buffer->fill < MEAN_ACCEL_BUFFER_SIZE) {
        mean_accel_buffer->fill++;
    }

    if (mean_accel_buffer->fill < MEAN_ACCEL_BUFFER_SIZE) {
        mean_accel_buffer->is_valid = false;
        return;
    }

    // We don't need to devide by the size of the buffer as we are calculating the unit vector afterwards anyway!
    copy_accel_vector(mean_accel_buffer->mean_unit_vector, mean_accel_buffer->sum);

    normalise_vector_to_unit_length(mean_accel_buffer->mean_unit_vector);

    mean_accel_buffer->is_valid = true;
}

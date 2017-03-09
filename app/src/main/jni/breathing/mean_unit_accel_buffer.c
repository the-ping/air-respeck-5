#include <malloc.h>
#include <stdio.h>
#include "mean_unit_accel_buffer.h"
#include "math_helper.h"

void initialise_mean_unit_accel_buffer(MeanUnitAccelBuffer *mean_accel_buffer, unsigned int buffer_size) {
    mean_accel_buffer->fill = 0;
    mean_accel_buffer->current_position = -1;
    mean_accel_buffer->is_valid = false;

    mean_accel_buffer->sum[0] = 0;
    mean_accel_buffer->sum[1] = 0;
    mean_accel_buffer->sum[2] = 0;

    mean_accel_buffer->buffer_size = buffer_size;

    // Allocate space for values array:
    mean_accel_buffer->values = calloc(buffer_size, sizeof(float *));

    for (int i = 0; i < mean_accel_buffer->buffer_size; i++) {
        mean_accel_buffer->values[i] = calloc(3, sizeof(float));
    }
}

void update_mean_unit_accel_buffer(float *new_accel_data, MeanUnitAccelBuffer *mean_accel_buffer) {
    // Increment position
    mean_accel_buffer->current_position = (mean_accel_buffer->current_position + 1) % mean_accel_buffer->buffer_size;

    subtract_from_accel_vector(mean_accel_buffer->sum, mean_accel_buffer->values[mean_accel_buffer->current_position]);
    copy_accel_vector(mean_accel_buffer->values[mean_accel_buffer->current_position], new_accel_data);
    add_to_accel_vector(mean_accel_buffer->sum, mean_accel_buffer->values[mean_accel_buffer->current_position]);

    if (mean_accel_buffer->fill < mean_accel_buffer->buffer_size) {
        mean_accel_buffer->fill++;
    }

    if (mean_accel_buffer->fill < mean_accel_buffer->buffer_size) {
        mean_accel_buffer->is_valid = false;
        return;
    }

    // We don't need to divide by the size of the buffer as we are calculating the unit vector afterwards anyway!
    copy_accel_vector(mean_accel_buffer->mean_unit_vector, mean_accel_buffer->sum);

    normalise_vector_to_unit_length(mean_accel_buffer->mean_unit_vector);

    mean_accel_buffer->is_valid = true;
}

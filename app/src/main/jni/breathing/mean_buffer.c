#include "mean_buffer.h"

void initialise_mean_buffer(MeanBuffer *mean_buffer) {
    mean_buffer->current_position = 0;
    mean_buffer->fill = 0;
    mean_buffer->is_valid = false;
    mean_buffer->sum = 0;

    for (int i = 0; i < MEAN_BUFFER_SIZE; i++) {
        mean_buffer->values[i] = 0;
    }
}

void update_mean_buffer(double value, MeanBuffer *mean_buffer) {

    // Update the buffer by subtracting the mean_unit_vector stored at the current position from the sum
    // (as that mean_unit_vector is overridden), storing the current mean_unit_vector, and then adding that back to the sum
    mean_buffer->sum -= mean_buffer->values[mean_buffer->current_position];
    mean_buffer->values[mean_buffer->current_position] = value;
    mean_buffer->sum += mean_buffer->values[mean_buffer->current_position];

    mean_buffer->current_position = (mean_buffer->current_position + 1) % MEAN_BUFFER_SIZE;

    if (mean_buffer->fill < MEAN_BUFFER_SIZE) {
        mean_buffer->fill++;
    }

    if (mean_buffer->fill < MEAN_BUFFER_SIZE) {
        mean_buffer->is_valid = false;
        return;
    }

    // If the buffer is filled, the sum devided by the size gives us the mean of the values in the buffer
    mean_buffer->value = mean_buffer->sum / MEAN_BUFFER_SIZE;
    mean_buffer->is_valid = true;
}

#include <malloc.h>
#include "mean_post_filter.h"

void initialise_mean_post_filter(MeanPostFilter *mean_buffer, bool isPostFilteringEnabled) {
    mean_buffer->current_position = -1;
    mean_buffer->fill = 0;
    mean_buffer->is_valid = false;
    mean_buffer->sum = 0;

    if (isPostFilteringEnabled) {
        mean_buffer->buffer_size = 12;
    } else {
        mean_buffer->buffer_size = 1;
    }
    // Allocate space for values array:
    mean_buffer->values = calloc(mean_buffer->buffer_size, sizeof(float *));

    for (int i = 0; i < mean_buffer->buffer_size; i++) {
        mean_buffer->values[i] = 0;
    }
}

void update_mean_post_filter(float value, MeanPostFilter *mean_buffer) {

    // Increment position
    mean_buffer->current_position = (mean_buffer->current_position + 1) % mean_buffer->buffer_size;

    // Update the buffer by subtracting the mean_unit_vector stored at the current position from the sum
    // (as that mean_unit_vector is overridden), storing the current mean_unit_vector, and then adding that back to the sum
    mean_buffer->sum -= mean_buffer->values[mean_buffer->current_position];
    mean_buffer->values[mean_buffer->current_position] = value;
    mean_buffer->sum += mean_buffer->values[mean_buffer->current_position];


    if (mean_buffer->fill < mean_buffer->buffer_size) {
        mean_buffer->fill++;
    }

    if (mean_buffer->fill < mean_buffer->buffer_size) {
        mean_buffer->is_valid = false;
        return;
    }

    // If the buffer is filled, the sum divided by the size gives us the mean of the values in the buffer
    mean_buffer->mean_value = mean_buffer->sum / mean_buffer->buffer_size;
    mean_buffer->is_valid = true;
}

//
// Created by Darius on 23.08.2017.
//

#include <malloc.h>
#include "vector_length_buffer.h"
#include "step_count.h"

void initialise_vector_length_buffer(VectorLengthBuffer *vector_length_buffer) {
    vector_length_buffer->fill = 0;
    vector_length_buffer->current_position = -1;
    vector_length_buffer->is_valid = false;
    vector_length_buffer->mean_length = 0.0;
    vector_length_buffer->sum = 0.0;
    vector_length_buffer->values = calloc(VECTOR_LENGTH_BUFFER_SIZE, sizeof(double));
}

void update_vector_length_buffer(double new_vector_length, VectorLengthBuffer *vector_length_buffer) {
    // Increment position
    vector_length_buffer->current_position = (vector_length_buffer->current_position + 1) % VECTOR_LENGTH_BUFFER_SIZE;

    // Update buffer and sum with current vector length
    vector_length_buffer->sum -= vector_length_buffer->values[vector_length_buffer->current_position];
    vector_length_buffer->values[vector_length_buffer->current_position] = new_vector_length;
    vector_length_buffer->sum += vector_length_buffer->values[vector_length_buffer->current_position];

    if (vector_length_buffer->fill < VECTOR_LENGTH_BUFFER_SIZE) {
        vector_length_buffer->fill++;
    }

    if (vector_length_buffer->fill < VECTOR_LENGTH_BUFFER_SIZE) {
        vector_length_buffer->is_valid = false;
        return;
    }

    vector_length_buffer->mean_length = vector_length_buffer->sum / VECTOR_LENGTH_BUFFER_SIZE;
    
    vector_length_buffer->is_valid = true;
}
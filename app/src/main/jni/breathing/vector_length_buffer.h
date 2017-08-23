//
// Created by Darius on 23.08.2017.
//

#ifndef AIRRESPECK_VECTOR_LENGTH_BUFFER_H
#define AIRRESPECK_VECTOR_LENGTH_BUFFER_H

#include <stdbool.h>

#define FILTER_LENGTH 3

typedef struct {

    int current_position, fill;
    float vector_lengths[FILTER_LENGTH];

    bool is_valid;
} VectorLengthBuffer;

void initialise_vector_length_buffer(VectorLengthBuffer *vector_length_buffer);

void update_vector_length_buffer(float *current_accel, VectorLengthBuffer *vector_length_buffer);

#endif //AIRRESPECK_VECTOR_LENGTH_BUFFER_H


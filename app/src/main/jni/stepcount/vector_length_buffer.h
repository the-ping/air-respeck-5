//
// Created by Darius on 23.08.2017.
//

#ifndef AIRRESPECK_VECTOR_LENGTH_BUFFER_H
#define AIRRESPECK_VECTOR_LENGTH_BUFFER_H

#include <stdbool.h>

#define VECTOR_LENGTH_BUFFER_SIZE 3

typedef struct {

    int current_position, fill;
    double *values;
    double mean_length;
    bool is_valid;
    double sum;

} VectorLengthBuffer;

void initialise_vector_length_buffer(VectorLengthBuffer *vector_length_buffer);

void update_vector_length_buffer(double new_vector_length, VectorLengthBuffer *vector_length_buffer);

#endif //AIRRESPECK_VECTOR_LENGTH_BUFFER_H


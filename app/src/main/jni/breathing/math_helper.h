#ifndef __MATH_HELPER_H__
#define __MATH_HELPER_H__

#include <stdint.h>

void normalise_vector_to_unit_length(float *vector);
void copy_accel_vector(float *out, float *in);
float dot_product(float *v, float *u);
void cross_product(float *out, float *in1, float *in2);
void subtract_from_accel_vector(float *minuend, float *subtrahend);
void add_to_accel_vector(float *summand1, float *summand2);
double calculate_vector_length(float *vector);
float mean(int *array, int length);

#endif

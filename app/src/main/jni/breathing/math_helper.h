#ifndef __MATH_HELPER_H__
#define __MATH_HELPER_H__

#include <stdint.h>

void normalise_vector_to_unit_length(double *vector);

void copy_accel_vector(double *out, double *in);
double dot_product(double *v, double *u);
void cross_product(double *out, double *in1, double *in2);
void subtract_from_accel_vector(double *minuend, double *subtrahend);
void add_to_accel_vector(double *summand1, double *summand2);

#endif

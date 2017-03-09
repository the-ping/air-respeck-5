#include "math_helper.h"
#include <math.h>

void normalise_vector_to_unit_length(float *vector) {
    float length = sqrt(dot_product(vector, vector));

    vector[0] /= length;
    vector[1] /= length;
    vector[2] /= length;
}

void copy_accel_vector(float *out, float *in) {
    out[0] = in[0];
    out[1] = in[1];
    out[2] = in[2];
}

void subtract_from_accel_vector(float *minuend, float *subtrahend) {
    minuend[0] -= subtrahend[0];
    minuend[1] -= subtrahend[1];
    minuend[2] -= subtrahend[2];
}

void add_to_accel_vector(float *summand1, float *summand2) {
    summand1[0] += summand2[0];
    summand1[1] += summand2[1];
    summand1[2] += summand2[2];
}


float dot_product(float *v, float *u) {
    return v[0] * u[0] + v[1] * u[1] + v[2] * u[2];
}


void cross_product(float *out, float *in1, float *in2) {
    out[0] = in1[1] * in2[2] - in1[2] * in2[1];
    out[1] = in1[2] * in2[0] - in1[0] * in2[2];
    out[2] = in1[0] * in2[1] - in1[1] * in2[0];
}


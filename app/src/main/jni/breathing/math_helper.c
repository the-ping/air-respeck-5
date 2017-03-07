#include "math_helper.h"
#include <math.h>

void normalise_vector_to_unit_length(double *vector) {
    double length = sqrt(dot_product(vector, vector));

    vector[0] /= length;
    vector[1] /= length;
    vector[2] /= length;
}

void copy_accel_vector(double *out, double *in) {
    out[0] = in[0];
    out[1] = in[1];
    out[2] = in[2];
}

double dot_product(double *v, double *u) {
    return v[0] * u[0] + v[1] * u[1] + v[2] * u[2];
}


void cross_product(double *out, double *in1, double *in2) {
    out[0] = in1[1] * in2[2] - in1[2] * in2[1];
    out[1] = in1[2] * in2[0] - in1[0] * in2[2];
    out[2] = in1[0] * in2[1] - in1[1] * in2[0];
}


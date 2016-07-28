#ifndef __MATH_HELPER_H__
#define __MATH_HELPER_H__

#include <stdint.h>

void normalize(double vector[3]);
void normalize_int_to_float(int32_t *vector, float *u);
void normalize_f32(float *u);

void vector_copy_dbl(double in[3], double out[3]);
void vector_copy_int(int32_t in[3], int32_t out[3]);
double dot(double v[3], double u[3]);
void cross(double r[3], double u[3], double v[3]);

#endif


#include "math_helper.h"

#include <math.h>
//#include "arm_math.h"

void normalize(double vector[3])
{

	double len = sqrt(dot(vector,vector));
    
    //double x = vector[0];

	vector[0] /= len;
	vector[1] /= len;
	vector[2] /= len;

}

/*
void normalize_int_to_float(int32_t *vector, float *u)
{
	// convert to new format
	u[0] = vector[0];
	u[1] = vector[1];
	u[2] = vector[2];

	// calculate dot
	float dot, len;
	arm_dot_prod_f32(u,u,3,&dot);
	arm_sqrt_f32(dot,&len);

	len = 1/len;

	arm_scale_f32(u,len,u,3);

}

void normalize_f32(float *u)
{

	float dot, len;
	arm_dot_prod_f32(u,u,3,&dot);
	arm_sqrt_f32(dot,&len);

	len = 1/len;

	arm_scale_f32(u,len,u,3);

}
 */

void vector_copy_dbl(double in[3], double out[3])
{

	out[0] = in[0];
	out[1] = in[1];
	out[2] = in[2];

}

void vector_copy_int(int32_t in[3], int32_t out[3])
{

	out[0] = in[0];
	out[1] = in[1];
	out[2] = in[2];

}

double dot(double v[3], double u[3])
{

	double d;

	d = v[0] * u[0] + v[1] * u[1] + v[2] * u[2];

	return d;

}


void cross(double r[3], double u[3], double v[3])
{
    r[0] = u[1]*v[2] - u[2]*v[1];
    r[1] = u[2]*v[0] - u[0]*v[2];
    r[2] = u[0]*v[1] - u[1]*v[0];
}


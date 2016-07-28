#ifndef __AXIS_AND_ANGLE_H__
#define __AXIS_AND_ANGLE_H__

#include <stdbool.h>
#include "math_helper.h"

typedef struct
{

	double prev_data[3];
	bool prev_data_valid;

	double value[3];
	bool valid;

} axis_and_angle_filter;

void AAA_init(axis_and_angle_filter* filter);
void AAA_update(double data[3], axis_and_angle_filter* filter);

#endif

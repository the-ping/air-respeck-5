#ifndef __MEAN_AXIS_H__
#define __MEAN_AXIS_H__

#include <stdbool.h>
#include <stdint.h>

#define MEAN_AXIS_SIZE 128

typedef struct
{

	double sum[3];
	int pos;
	int fill;

	double values[MEAN_AXIS_SIZE][3];
	double value[3];
	bool valid;

} mean_axis_filter;

void MAX_init(mean_axis_filter* filter);
void MAX_update(double data[3], mean_axis_filter* filter);

#endif

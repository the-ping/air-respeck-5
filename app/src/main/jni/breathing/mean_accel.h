#ifndef __MEAN_ACCEL_H__
#define __MEAN_ACCEL_H__

#include <stdbool.h>
#include <stdint.h>

#define MEAN_ACCEL_SIZE 128

typedef struct
{

	double sum[3];
	int pos;
	int fill;

	double values[MEAN_ACCEL_SIZE][3];
	double value[3];
	bool valid;

} mean_accel_filter;

void MAC_init(mean_accel_filter* filter);
void MAC_update(double data[3], mean_accel_filter* filter);

#endif

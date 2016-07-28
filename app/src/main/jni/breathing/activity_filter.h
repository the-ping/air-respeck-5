#ifndef __ACTIVITY_FILTER_H__
#define __ACTIVITY_FILTER_H__

#include <stdint.h>
#include <stdbool.h>

#define ACTIVITY_BUFFER_SIZE 32

typedef struct
{

	int pos, fill;
	float values[ACTIVITY_BUFFER_SIZE];

	double prev_accel[3];
	bool prev_accel_valid;

	float max;
	bool valid;

} activity_filter;

void ACT_init(activity_filter *filter);
void ACT_update(double accel[3], activity_filter *filter);

#endif

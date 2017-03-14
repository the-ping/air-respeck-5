#ifndef __MEAN_BUFFER_H__
#define __MEAN_BUFFER_H__

#include <stdbool.h>
#include <stdint.h>

#define MEAN_ACCEL_FILTER_SIZE 12

typedef struct
{

	float sum;
	int current_position;
	int fill;

	float values[MEAN_ACCEL_FILTER_SIZE];
	float mean_value;
	bool is_valid;

} MeanFilter;

void initialise_mean_filter(MeanFilter *mean_buffer);
void update_mean_filter(float value, MeanFilter *mean_buffer);

#endif

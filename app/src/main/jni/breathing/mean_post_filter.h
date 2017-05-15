#ifndef __MEAN_BUFFER_H__
#define __MEAN_BUFFER_H__

#include <stdbool.h>
#include <stdint.h>

#define MEAN_ACCEL_FILTER_SIZE 1

typedef struct
{

	float sum;
	int current_position;
	int fill;

	float values[MEAN_ACCEL_FILTER_SIZE];
	float mean_value;
	bool is_valid;

} MeanPostFilter;

void initialise_mean_post_filter(MeanPostFilter *mean_buffer);
void update_mean_post_filter(float value, MeanPostFilter *mean_buffer);

#endif

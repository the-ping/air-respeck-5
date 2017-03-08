#ifndef __MEAN_BUFFER_H__
#define __MEAN_BUFFER_H__

#include <stdbool.h>
#include <stdint.h>

#define MEAN_ACCEL_FILTER_SIZE 12

typedef struct
{

	double sum;
	int current_position;
	int fill;

	double values[MEAN_ACCEL_FILTER_SIZE];
	double value;
	bool is_valid;

} MeanBuffer;

void initialise_mean_buffer(MeanBuffer *mean_buffer);
void update_mean_buffer(double value, MeanBuffer *mean_buffer);

#endif

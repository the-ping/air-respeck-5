#ifndef __MEAN_AXIS_BUFFER_H__
#define __MEAN_AXIS_BUFFER_H__

#include <stdbool.h>
#include <stdint.h>

#define MEAN_AXIS_SIZE 128

typedef struct
{

	float sum[3];
	int current_position;
	int fill;

	float accel_buffer[MEAN_AXIS_SIZE][3];
	float mean_axis[3];
	bool is_valid;

} MeanAxisBuffer;

void initialise_mean_axis_buffer(MeanAxisBuffer *mean_axis_buffer);
void update_mean_axis_buffer(float *new_accel_data, MeanAxisBuffer *mean_axis_buffer);

#endif

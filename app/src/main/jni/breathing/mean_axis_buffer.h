#ifndef __MEAN_AXIS_BUFFER_H__
#define __MEAN_AXIS_BUFFER_H__

#include <stdbool.h>
#include <stdint.h>

#define MEAN_AXIS_SIZE 128

typedef struct
{

	double sum[3];
	int pos;
	int fill;

	double accel_data[MEAN_AXIS_SIZE][3];
	double value[3];
	bool is_valid;

} MeanAxisBuffer;

void initialise_mean_axis_buffer(MeanAxisBuffer *mean_axis_buffer);
void update_mean_axis_buffer(double *new_accel_data, MeanAxisBuffer *mean_axis_buffer);

#endif

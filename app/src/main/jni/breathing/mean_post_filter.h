#ifndef __MEAN_BUFFER_H__
#define __MEAN_BUFFER_H__

#include <stdbool.h>
#include <stdint.h>

typedef struct
{

	float sum;
	int current_position;
	int fill;
    unsigned int buffer_size;

	float *values;
	float mean_value;
	bool is_valid;

} MeanPostFilter;

void initialise_mean_post_filter(MeanPostFilter *mean_buffer, unsigned int post_filter_size);
void update_mean_post_filter(float value, MeanPostFilter *mean_buffer);

#endif

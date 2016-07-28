#ifndef __MEAN_AVERAGE_FILTER_H__
#define __MEAN_AVERAGE_FILTER_H__

#include <stdbool.h>
#include <stdint.h>

#define MEAN_AVERAGE_SIZE 12

typedef struct
{

	double sum[3];
	int pos;
	int fill;

	double values[MEAN_AVERAGE_SIZE][3];
	double value[3];
	bool valid;

} mean_average_filter;

void MAF_init(mean_average_filter* filter);
void MAF_update(double value[3], mean_average_filter* filter);

#endif

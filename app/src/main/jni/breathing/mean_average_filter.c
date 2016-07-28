
#include "mean_average_filter.h"

//#include "math_helper.h"

#include <stdbool.h>
#include <stdint.h>

// TODO: consider using running totals

void MAF_init(mean_average_filter* filter)
{
	filter->fill = 0;
	filter->pos = 0;
	filter->valid = false;

	filter->sum[0] = 0;
	filter->sum[1] = 0;
	filter->sum[2] = 0;

	int i;
	for (i = 0; i < MEAN_AVERAGE_SIZE; i++)
	{
		filter->values[i][0] = 0;
		filter->values[i][1] = 0;
		filter->values[i][2] = 0;
	}


}

void MAF_update(double value[3], mean_average_filter* filter)
{
    //filter->sum[0] += 1.0;
    //return;

	//vector_copy_dbl(value,filter->values[filter->pos]);
	filter->sum[0] -= filter->values[filter->pos][0];
	filter->sum[1] -= filter->values[filter->pos][1];
	filter->sum[2] -= filter->values[filter->pos][2];

	filter->values[filter->pos][0] = value[0];
	filter->values[filter->pos][1] = value[1];
	filter->values[filter->pos][2] = value[2];

	filter->sum[0] += filter->values[filter->pos][0];
	filter->sum[1] += filter->values[filter->pos][1];
	filter->sum[2] += filter->values[filter->pos][2];

	filter->pos = (filter->pos + 1) % MEAN_AVERAGE_SIZE;

	if (filter->fill < MEAN_AVERAGE_SIZE)
	{
		filter->fill++;
	}

	if (filter->fill < MEAN_AVERAGE_SIZE)
	{
		filter->valid = false;
		return;
	}

	/*
	filter->value[0] = 0;
	filter->value[1] = 0;
	filter->value[2] = 0;
	uint8_t i;

	for (i = 0; i < MEAN_AVERAGE_SIZE; i++)
	{

		filter->value[0] += filter->values[i][0];
		filter->value[1] += filter->values[i][1];
		filter->value[2] += filter->values[i][2];

	}
	*/

	filter->value[0] = filter->sum[0] / MEAN_AVERAGE_SIZE;
	filter->value[1] = filter->sum[1] / MEAN_AVERAGE_SIZE;
	filter->value[2] = filter->sum[2] / MEAN_AVERAGE_SIZE;

	//strange line not in iOS: filter->value[0] = filter->fill;

	filter->valid = true;

}

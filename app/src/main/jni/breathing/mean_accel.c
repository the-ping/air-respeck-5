
#include "mean_accel.h"

#include "math_helper.h"
//#include "arm_math.h"

void MAC_init(mean_accel_filter* filter)
{

	filter->fill = 0;
	filter->pos = 0;
	filter->valid = 0;

	filter->sum[0] = 0;
	filter->sum[1] = 0;
	filter->sum[2] = 0;

	int i;
	for (i = 0; i < MEAN_ACCEL_SIZE; i++)
	{
		filter->values[i][0] = 0;
		filter->values[i][1] = 0;
		filter->values[i][2] = 0;
	}

}

// TODO: speed increase using running totals
void MAC_update(double data[3], mean_accel_filter* filter)
{

	//vector_copy_dbl(data,filter->values[filter->pos]);
	//arm_sub_f32(filter->sum,filter->values[filter->pos],filter->sum,3);
    filter->sum[0] = filter->sum[0] - filter->values[filter->pos][0];
    filter->sum[1] = filter->sum[1] - filter->values[filter->pos][1];
    filter->sum[2] = filter->sum[2] - filter->values[filter->pos][2];

    
	//arm_copy_f32(data,filter->values[filter->pos],3);
    filter->values[filter->pos][0] = data[0];
    filter->values[filter->pos][1] = data[1];
    filter->values[filter->pos][2] = data[2];

	//arm_add_f32(filter->sum,filter->values[filter->pos],filter->sum,3);
    filter->sum[0] = filter->sum[0] + filter->values[filter->pos][0];
    filter->sum[1] = filter->sum[1] + filter->values[filter->pos][1];
    filter->sum[2] = filter->sum[2] + filter->values[filter->pos][2];

	filter->pos = (filter->pos + 1) % MEAN_ACCEL_SIZE;

	if (filter->fill < MEAN_ACCEL_SIZE)
	{
		filter->fill++;
	}

	if (filter->fill < MEAN_ACCEL_SIZE)
	{
		filter->valid = false;
		return;
	}

	/*
	filter->value[0] = 0;
	filter->value[1] = 0;
	filter->value[2] = 0;

	uint8_t i;

	for(i = 0; i < MEAN_AXIS_SIZE; i++)
	{

		filter->value[0] += filter->values[i][0];
		filter->value[1] += filter->values[i][1];
		filter->value[2] += filter->values[i][2];

	}
	*/

	//arm_copy_f32(filter->sum,filter->value,3);
    filter->value[0] = filter->sum[0];
    filter->value[1] = filter->sum[1];
    filter->value[2] = filter->sum[2];
    

	//normalize_f32(filter->value);
    normalize(filter->value);

	filter->valid = true;

}

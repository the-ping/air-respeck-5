
#include "mean_axis.h"

#include "math_helper.h"
//#include "arm_math.h"

void get_ref_axis(double ref[3]);

void MAX_init(mean_axis_filter* filter)
{

	filter->fill = 0;
	filter->pos = 0;
	filter->valid = 0;

	filter->sum[0] = 0;
	filter->sum[1] = 0;
	filter->sum[2] = 0;

	int i;
	for (i = 0; i < MEAN_AXIS_SIZE; i++)
	{
		filter->values[i][0] = 0;
		filter->values[i][1] = 0;
		filter->values[i][2] = 0;
	}

}

// TODO: speed increase using running totals
void MAX_update(double data[3], mean_axis_filter* filter)
{

	double ref[3], dot_res;
	get_ref_axis(ref);

	//vector_copy_dbl(data,filter->values[filter->pos]);
	//arm_sub_f32(filter->sum,filter->values[filter->pos],filter->sum,3);
    filter->sum[0] = filter->sum[0] - filter->values[filter->pos][0];
    filter->sum[1] = filter->sum[1] - filter->values[filter->pos][1];
    filter->sum[2] = filter->sum[2] - filter->values[filter->pos][2];

    
	//arm_copy_f32(data,filter->values[filter->pos],3);
    filter->values[filter->pos][0] = data[0];
    filter->values[filter->pos][1] = data[1];
    filter->values[filter->pos][2] = data[2];

	//arm_dot_prod_f32(data,ref,3,&dot_res);
    dot_res = dot(data,ref);
    

	if (dot_res < 0)
	{
		filter->values[filter->pos][0] *= -1.0;
		filter->values[filter->pos][1] *= -1.0;
		filter->values[filter->pos][2] *= -1.0;
		

		//arm_scale_f32(filter->values[filter->pos], -1.0, filter->values[filter->pos], 3);

	}

	//arm_add_f32(filter->sum,filter->values[filter->pos],filter->sum,3);
    filter->sum[0] = filter->sum[0] + filter->values[filter->pos][0];
    filter->sum[1] = filter->sum[1] + filter->values[filter->pos][1];
    filter->sum[2] = filter->sum[2] + filter->values[filter->pos][2];

	filter->pos = (filter->pos + 1) % MEAN_AXIS_SIZE;

	if (filter->fill < MEAN_AXIS_SIZE)
	{
		filter->fill++;
	}

	if (filter->fill < MEAN_AXIS_SIZE)
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

void get_ref_axis(double ref[3])
{

	ref[0] = 0.98499424;
	ref[1] = -0.17221591;
	ref[2] = 0.01131468;

}


#include "axis_and_angle.h"


void AAA_init(axis_and_angle_filter* filter)
{
	filter->valid = false;
	filter->prev_data_valid = false;
}

void AAA_update(double data[3], axis_and_angle_filter* filter)
{

	if (filter->prev_data_valid == false)
	{

		//arm_copy_q31(data,filter->prev_data,3);
        filter->prev_data[0] = data[0];
        filter->prev_data[1] = data[1];
        filter->prev_data[2] = data[2];

		filter->prev_data_valid = true;
		filter->valid = false;

		return;

	}

	
	filter->value[0] = filter->prev_data[1] * data[2] - filter->prev_data[2] * data[1];
	filter->value[1] = -(filter->prev_data[0] * data[2] - filter->prev_data[2] * data[0]);
	filter->value[2] = filter->prev_data[0] * data[1] - filter->prev_data[1] * data[0];
    
    filter->prev_data[0] = data[0];
    filter->prev_data[1] = data[1];
    filter->prev_data[2] = data[2];
    

	filter->valid = true;

}


#include "breathing.h"

#include <stdbool.h>
#include <math.h>

#include "mean_average_filter.h"
#include "axis_and_angle.h"
#include "average.h"
#include "mean_axis.h"
#include "mean_accel.h"
#include "activity_filter.h"
#include "math_helper.h"
#include "../activityclassification/predictions.h"

//#include "arm_math.h"

mean_average_filter input_filter;
axis_and_angle_filter aaa_filter;
mean_axis_filter axis_filter;
mean_accel_filter accel_filter;
average_filter average;
average_filter average2;
activity_filter activity;

void BRG_init(breathing_filter* filter)
{

	// mean average filter
	MAF_init(&input_filter);
	// axis and angle filter
	AAA_init(&aaa_filter);
	// mean axis filter
	MAX_init(&axis_filter);
    // mean accel filter
    MAC_init(&accel_filter);
	// average filter
	AVG_init(&average);
    AVG_init(&average2);
	// activity filter
	//ACT_init(&activity);

	filter->valid = false;
	filter->sample_rate_valid = false;
	filter->bs = NAN;
	filter->ba = NAN;
	filter->activity = NAN;
}

void BRG_update(double value[3], breathing_filter* filter)
{
	if (filter->sample_rate_valid == false)
	{
		filter->valid = false;
		// filter->bs = 1.0;
		return;
	}
    
    if (isnan(value[0]) || isnan(value[1]) || isnan(value[2]))
    {
        filter->valid = false;
        // filter->bs = 2.0;
        return;
    }

	double accel[3];
	accel[0] = value[0];
	accel[1] = value[1];
	accel[2] = value[2];

	filter->valid = false;

	int previous_pos = activity.pos;

    // activity (movement detection)
	ACT_update(accel,&activity);

	// Update classification buffer -> we want the actual activity level, not the maximum
	update_act_class_buffer(accel, activity.values[previous_pos]);
    
	if (activity.valid == false)
	{
		filter->valid = false;
		//filter->bs = 3.0;
		return;
	}
    
	filter->activity = activity.max;
    
	if (activity.max > ACTIVITY_CUTOFF)
	{
		filter->valid = false;
        filter->bs = NAN;
        //filter->bs = 4.0;
		return;
	}

	/* mean average filter */
	MAF_update(accel, &input_filter);

	if (input_filter.valid == false)
	{
		filter->valid = false;
		//filter->bs = input_filter.sum[0];
		return;
	}

	vector_copy_dbl(input_filter.value,accel);

	/* normalize */
	//double norm_accel[3];
	//normalize_int_to_float(accel, axis); // using arm_f32 functions

    normalize(accel);

	/* convert to q31 */
	//q31_t q31_axis[3];
	//arm_float_to_q31(axis,q31_axis,3);

	AAA_update(accel, &aaa_filter);

	if (aaa_filter.valid == false)
	{
		filter->valid = false;
		//filter->bs = 6.0;
		return;
	}

	/* the activity code needs optimising - it massively increases computation time because there is a lot of floating point operations */

    MAC_update(accel, &accel_filter);

	//arm_q31_to_float(aaa_filter.value,axis,3);

	MAX_update(aaa_filter.value,&axis_filter);

	if (axis_filter.valid == false)
	{
		filter->valid = false;
		//filter->bs = 7.0;
		return;
	}

	/* final breathing signal calculation */
	double final_bs;

	//arm_dot_prod_f32(axis_filter.value,accel,3,&final_rate);
    final_bs = dot(aaa_filter.value, axis_filter.value);
	//arm_scale_f32(&final_rate,filter->sample_rate,&final_rate,3);
    final_bs = final_bs * filter->sample_rate * 10.0f;
    

    /* final breathing angle calculation */
    
    double mean_accel_cross_mean_axis[3];
    cross(mean_accel_cross_mean_axis, accel_filter.value, axis_filter.value);
    double final_ba;
    final_ba = dot(mean_accel_cross_mean_axis, accel);
    
    AVG_update(final_bs,&average);
    AVG_update(final_ba,&average2);


	if (average.valid == false)
	{
		filter->valid = false;
		//filter->valid = 8.0;
		return;
	}

    // update the breathing signal and breathing angle
    filter->bs = average.value;
    filter->ba = average2.value;
    filter->valid = true;

}

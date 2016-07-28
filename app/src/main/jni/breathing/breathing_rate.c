//
//  breathing.c
//  TemperatureSensor
//
//  Created by andy on 16/03/2013.
//  Copyright (c) 2013 Apple Inc. All rights reserved.
//

#include "breathing_rate.h"

void threshold_init(threshold_filter *filter){
    filter->fill = 0;
    filter->pos = 0;
    filter->valid = false;
    filter->lower_value_sum = 0;
    filter->upper_value_sum = 0;
    filter->upper_value_sum_fill = 0;
    filter->lower_value_sum_fill = 0;
    
    int i;
    for (i = 0; i < THRESHOLD_FILTER_SIZE; i++)
        filter->values_type[i] = INVALID;

}

void update_threshold(float value, threshold_filter *filter)
{
    
	if (filter->values_type[filter->pos] == POSITIVE)
	{
		filter->upper_value_sum -= filter->values[filter->pos];
		filter->upper_value_sum_fill--;
	}
	else if (filter->values_type[filter->pos] == NEGATIVE)
	{
		filter->lower_value_sum -= filter->values[filter->pos];
		filter->lower_value_sum_fill--;
	}
    
	if (isnan(value) == true)
	{
		filter->values_type[filter->pos] = INVALID;
	}
	else
	{
        float squared_value = value * value;
        filter->values[filter->pos] = squared_value;
        
		if (value >= 0)
		{
			filter->upper_value_sum_fill++;
			filter->values_type[filter->pos] = POSITIVE;
            filter->upper_value_sum += squared_value;
		}
		else
		{
			filter->lower_value_sum_fill++;
			filter->values_type[filter->pos] = NEGATIVE;
            filter->lower_value_sum += squared_value;
		}
		//arm_mult_f32(&value.rate,&value.rate,&filter->values[filter->pos],1);
        
		
	}
    
	filter->pos = (filter->pos + 1) % THRESHOLD_FILTER_SIZE;
    
	if (filter->fill < THRESHOLD_FILTER_SIZE)
	{
		filter->fill++;
	}
    
	if (filter->fill < THRESHOLD_FILTER_SIZE)
	{
		filter->valid = false;
		return;
	}
    
	if (filter->upper_value_sum_fill > 0)
	{
		filter->upper_value = sqrt(filter->upper_value_sum / filter->upper_value_sum_fill);
		//arm_sqrt_f32(filter->upper_value,&filter->upper_value);
	}
	else
		filter->upper_value = NAN;
    
	if (filter->lower_value_sum_fill > 0)
    {
        filter->lower_value = -sqrt(filter->lower_value_sum / filter->lower_value_sum_fill);
        //arm_sqrt_f32(filter->lower_value,&filter->lower_value);
        //arm_scale_f32(&filter->lower_value,-1.0,&filter->lower_value,1);
    }
    else
        filter->lower_value = NAN;
    
	filter->valid = true;
    
}

void bpm_init(bpm_filter *filter){
    filter->state = UNKNOWN;
    filter->bpm = NAN;
    filter->min_threshold = 0.01;
    filter->max_threshold = 0.5;
    filter->sample_count = 0;
    filter->sample_rate = 12.5;
    filter->sample_count_valid = false;
    filter->updated = false;
}


void bpm_update(float value, float ut, float lt, bpm_filter *filter)
{
	//filter->bpm = NAN;
	filter->sample_count++;
    
	if (isnan(ut) || isnan(lt))
	{
        filter->bpm = NAN;
        filter->sample_count_valid = false;
		return;
	}
    
	if (isnan(value))
	{
        filter->bpm = NAN;
        filter->sample_count_valid = false;
		return;
	}
    
	// set initial state, if required
	if (filter->state == UNKNOWN)
	{
		if (value < lt)
		{
			filter->state = LOW;
		}
		else if (value > ut)
		{
			filter->state = HIGH;
		}
		else
		{
			filter->state = MID_UNKNOWN;
		}
        
	}
    
	if (ut - lt < filter->min_threshold * 2.0f)
	{
		filter->state = UNKNOWN;
        filter->bpm = NAN;
        filter->sample_count_valid = false;
		return;
	}
    
    //if (ut - lt > filter->max_threshold * 2.0f)
	//{
	//	filter->state = UNKNOWN;
    //    filter->bpm = NAN;
    //    filter->sample_count_valid = false;
	//	return;
	//}
    
	if (filter->state == LOW && value > lt)
	{
		filter->state = MID_RISING;
	}
	else if (filter->state == HIGH && value < ut)
	{
		filter->state = MID_FALLING;
	}
	else if ((filter->state == MID_RISING || filter->state == MID_UNKNOWN) && value > ut)
	{
		filter->state = HIGH;
	}
	else if ((filter->state == MID_FALLING || filter->state == MID_UNKNOWN) && value < lt)
	{
		filter->state = LOW;
        
        if (filter->sample_count_valid){
            filter->bpm = 60.0 * filter->sample_rate / (float)filter->sample_count;
        }
        
		filter->sample_count = 0;
        filter->sample_count_valid = true;
        filter->updated = true;
        
	}
}

#include "average.h"

void AVG_init(average_filter* filter)
{
	filter->pos = 0;
	filter->fill = 0;
	filter->valid = false;
	filter->sum = 0;

	uint8_t i;
	for (i = 0; i < AVERAGE_SIZE; i++)
	{

		filter->values[i] = 0;

	}
}

void AVG_update(float value, average_filter* filter)
{

	filter->sum -= filter->values[filter->pos];
	filter->values[filter->pos] = value;
	filter->sum += filter->values[filter->pos];

	filter->pos = (filter->pos + 1) % AVERAGE_SIZE;

	if (filter->fill < AVERAGE_SIZE)
	{
		filter->fill++;
	}

	if (filter->fill < AVERAGE_SIZE)
	{
		filter->valid = false;
		return;
	}


	filter->value = filter->sum / AVERAGE_SIZE;
	filter->valid = true;

}

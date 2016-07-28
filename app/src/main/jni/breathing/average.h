#ifndef __AVERAGE_H__
#define __AVERAGE_H__

#include <stdbool.h>
#include <stdint.h>

#define AVERAGE_SIZE 12

typedef struct
{

	float sum;
	uint8_t pos;
	uint8_t fill;

	float values[AVERAGE_SIZE];
	float value;
	bool valid;

} average_filter;

void AVG_init(average_filter* filter);
void AVG_update(float value, average_filter* filter);

#endif

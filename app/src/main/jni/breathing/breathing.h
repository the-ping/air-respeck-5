#ifndef __BREATHING_H__
#define __BREATHING_H__

#define SAMPLE_RATE 12.5
#define ACTIVITY_CUTOFF 0.3

#include <stdint.h>
#include <stdbool.h>

typedef struct
{
	float bs;
    float ba;
	bool valid;
	float activity;
	float sample_rate;
	bool sample_rate_valid;

} breathing_filter;

void BRG_init(breathing_filter* filter);
void BRG_update(double value[3], breathing_filter* filter);

#endif

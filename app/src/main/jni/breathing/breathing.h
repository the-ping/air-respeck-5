#ifndef __BREATHING_H__
#define __BREATHING_H__

#define SAMPLE_RATE 12.5
#define ACTIVITY_CUTOFF 0.3

#include <stdint.h>
#include <stdbool.h>

typedef struct
{
	double signal;
	double angle;
	bool is_valid;
	double max_act_level;
	bool is_breathing_initialised;

} BreathingBuffer;

void initialise_breathing_buffer(BreathingBuffer *breathing_buffer);
void update_breathing(double *new_accel_data_original, BreathingBuffer *breathing_buffer);

#endif

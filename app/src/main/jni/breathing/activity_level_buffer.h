#ifndef __ACTIVITY_FILTER_H__
#define __ACTIVITY_FILTER_H__

#include <stdint.h>
#include <stdbool.h>

#define ACTIVITY_LEVEL_BUFFER_SIZE 32

/**
 * This class is used for storing recent max_act_level level values which are needed to calculate the maximum max_act_level level.
 * This max_act_level level is used as an indicator for the current movement which is applied as a threshold
 * mechanism to filter out bad signal periods.
 */

typedef struct
{

	int current_position, fill;
	float activity_levels[ACTIVITY_LEVEL_BUFFER_SIZE];

	float previous_accel[3];
	bool previous_accel_valid;

	float max;
	bool is_valid;

} ActivityLevelBuffer;

void initialise_activity_level_buffer(ActivityLevelBuffer *act_level_buffer);
void update_activity_level_buffer(float *current_accel, ActivityLevelBuffer *act_level_buffer);

#endif

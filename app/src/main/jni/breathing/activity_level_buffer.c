#include "activity_level_buffer.h"
#include "math_helper.h"

#include <math.h>

void initialise_activity_level_buffer(ActivityLevelBuffer *act_level_buffer) {
    act_level_buffer->fill = 0;
    act_level_buffer->current_position = 0;
    act_level_buffer->is_valid = false;
    act_level_buffer->prev_accel_valid = false;
}

void update_activity_level_buffer(double *current_accel, ActivityLevelBuffer *act_level_buffer) {

    // If any of the acceleration values is nan (which shouldn't happen), we do not store them in the buffer
    if (isnan(current_accel[0]) || isnan(current_accel[1]) || isnan(current_accel[2])) {
        act_level_buffer->is_valid = false;
        return;
    }

    // If we don't have any previous values, set the previous values to equal the current ones
    if (act_level_buffer->prev_accel_valid == false) {
        copy_accel_vector(act_level_buffer->previous_accel, current_accel);
        act_level_buffer->prev_accel_valid = true;
        act_level_buffer->is_valid = false;
        return;
    }

    // Caluclate the current activity level
    double current_act_level = sqrt((current_accel[0] - act_level_buffer->previous_accel[0]) *
                                            (current_accel[0] - act_level_buffer->previous_accel[0]) +
             (current_accel[1] - act_level_buffer->previous_accel[1]) * (current_accel[1] - act_level_buffer->previous_accel[1]) +
             (current_accel[2] - act_level_buffer->previous_accel[2]) * (current_accel[2] - act_level_buffer->previous_accel[2]));
    act_level_buffer->values[act_level_buffer->current_position] = current_act_level;
    act_level_buffer->current_position = (act_level_buffer->current_position + 1) % ACTIVITY_LEVEL_BUFFER_SIZE;

    // Increase the fill level
    if (act_level_buffer->fill < ACTIVITY_LEVEL_BUFFER_SIZE) {
        act_level_buffer->fill++;
    }

    // If the max_act_level array hasn't been filled yet, the act_level_buffer cannot be used.
    if (act_level_buffer->fill < ACTIVITY_LEVEL_BUFFER_SIZE) {
        act_level_buffer->is_valid = false;
        return;
    }

    // If the buffer is full, we calculate the maximum activity level in the buffer
    act_level_buffer->max = act_level_buffer->values[0];
    for (int i = 1; i < ACTIVITY_LEVEL_BUFFER_SIZE; i++) {
        if (act_level_buffer->values[i] > act_level_buffer->max) {
            act_level_buffer->max = act_level_buffer->values[i];
        }
    }

    // Store the current acceleration values as the new previous values
    copy_accel_vector(act_level_buffer->previous_accel, current_accel);
    act_level_buffer->is_valid = true;
}

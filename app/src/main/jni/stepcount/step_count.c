//
// Created by Darius on 23.08.2017.
//

#include <math.h>
#include "step_count.h"
#include "../breathing/math_helper.h"

void initialise_stepcounter(StepCounter *step_counter) {
    initialise_vector_length_buffer(&step_counter->vector_length_buffer);

    step_counter->previous_vector_length = 0;
    step_counter->samples_since_last_step = 0;
    step_counter->num_valid_steps = 0;
    step_counter->num_samples_until_static = 0;
    step_counter->minute_step_count = 0;
}

void update_stepcounter(float *new_accel_data, StepCounter *step_counter) {
    // Calculate the mean of VECTOR_LENGTH_BUFFER_SIZE vector lengths
    double vector_length = calculate_vector_length(new_accel_data);
    update_vector_length_buffer(vector_length, &step_counter->vector_length_buffer);
    double mean_length = step_counter->vector_length_buffer.mean_length;

    if (step_counter->vector_length_buffer.is_valid) {

        step_counter->samples_since_last_step += 1;

        // Check if there is a threshold crossing and that the last step was registered recently
        if (mean_length > STEP_THRESHOLD && step_counter->previous_vector_length <= STEP_THRESHOLD &&
            step_counter->samples_since_last_step > STEP_MIN_DELAY_SAMPLES) {

            // Reset step timer: if there is no step registered within NUM_SAMPLES_UNTIL_STATIC samples, we
            // reset to STATIC state
            step_counter->num_samples_until_static = NUM_SAMPLES_UNTIL_STATIC;

            // We have a potentially valid step. If we were static before, start moving and keep track of valid steps
            if (step_counter->current_state == STATIC) {
                step_counter->num_valid_steps = 1;
                step_counter->current_state = MOVING;
            } else if (step_counter->current_state == MOVING) {
                // If we were already in moving state, take note of this valid step
                step_counter->num_valid_steps += 1;

                // Keep track of step time differences after second step
                step_counter->step_distances[step_counter->num_valid_steps - 2] =
                        step_counter->samples_since_last_step;

                if (step_counter->num_valid_steps == NUM_STEPS_UNTIL_COUNT_WALKING) {
                    // We have the number of steps required for walking. Only register walking if the steps were
                    // in regular time intervals
                    float mean_distance = mean(step_counter->step_distances, NUM_STEPS_UNTIL_COUNT_WALKING - 1);
                    bool valid_walking = true;
                    // If one of the distances deviates a lot from the mean, then the "steps" were probably not
                    // due to walking
                    for (int i = 0; i < NUM_STEPS_UNTIL_COUNT_WALKING - 1; i += 1) {
                        if (fabs(mean_distance - step_counter->step_distances[i]) >
                            MAX_ALLOWED_DEVIATION_FROM_MEAN_DISTANCE) {
                            valid_walking = false;
                        }
                    }

                    if (valid_walking) {
                        step_counter->current_state = WALKING;
                        step_counter->minute_step_count += step_counter->num_valid_steps;
                    } else {
                        // Reset to static state
                        step_counter->current_state = STATIC;
                    }
                }
            } else {
                // State is walking
                step_counter->minute_step_count += 1;
            }

            step_counter->samples_since_last_step = 0;
        } else {
            if (step_counter->current_state != STATIC) {
                // We are not above the threshold. If this happens often, fall back to static state
                step_counter->num_samples_until_static -= 1;
                if (step_counter->num_samples_until_static == 0) {
                    step_counter->current_state = STATIC;
                }
            }
        }
        step_counter->previous_vector_length = mean_length;
    }
}

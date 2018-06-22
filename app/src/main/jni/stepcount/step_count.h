//
// Created by Darius on 23.08.2017.
//

#ifndef AIRRESPECK_STEP_COUNT_H
#define AIRRESPECK_STEP_COUNT_H

#include "vector_length_buffer.h"

#define STEP_THRESHOLD 1.02
#define STEP_MIN_DELAY_SAMPLES 2
#define NUM_STEPS_UNTIL_COUNT_WALKING 6
#define MAX_ALLOWED_DEVIATION_FROM_MEAN_DISTANCE 6
#define NUM_SAMPLES_UNTIL_STATIC 20

enum State {
    STATIC, MOVING, WALKING
};

typedef struct {
    double previous_vector_length;

    int step_distances[NUM_STEPS_UNTIL_COUNT_WALKING - 1];

    int samples_since_last_step;
    int num_valid_steps;
    int num_samples_until_static;
    int minute_step_count;

    VectorLengthBuffer vector_length_buffer;

    enum State current_state;

} StepCounter;

void initialise_stepcounter(StepCounter *step_counter);

void update_stepcounter(float *new_accel_data, StepCounter *step_counter);

bool is_walking(StepCounter *step_counter);

bool is_moving(StepCounter *step_counter);

#endif //AIRRESPECK_STEP_COUNT_H

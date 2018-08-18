//
// Created by Darius on 11.01.2017.
//

#ifndef __PREDICTIONS_H__
#define __PREDICTIONS_H__

#define ACTIVITY_STAND_SIT 0
#define ACTIVITY_WALKING 1
#define ACTIVITY_LYING 2
#define ACTIVITY_WRONG_ORIENTATION 3
#define ACTIVITY_SITTING_BENT_FORWARD 4
#define ACTIVITY_SITTING_BENT_BACKWARD 5
#define ACTIVITY_LYING_DOWN_RIGHT 6
#define ACTIVITY_LYING_DOWN_LEFT 7
#define ACTIVITY_LYING_DOWN_STOMACH 8
#define ACTIVITY_MOVEMENT 9

#include <stdbool.h>
#include "../stepcount/step_count.h"

typedef struct {
    int last_prediction;
} ActivityPredictor;

void initialise_activity_classification(ActivityPredictor *activity_predictor);

void update_activity_classification(ActivityPredictor *activity_predictor, float *accel, StepCounter *step_counter);

int get_advanced_activity_prediction(float x, float y, float z, bool is_walking, bool is_moving);

#endif

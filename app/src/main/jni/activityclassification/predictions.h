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

#define ACT_CLASS_BUFFER_SIZE 50

#include <stdbool.h>
#include "../stepcount/step_count.h"

typedef struct {
    int last_prediction;
    float act_class_buffer[ACT_CLASS_BUFFER_SIZE][2];
    int current_idx_in_buffer;
    bool is_buffer_full;
    bool stepcount_is_walking;
} ActivityPredictor;

void initialise_activity_classification(ActivityPredictor *activity_predictor);
void update_activity_classification_buffer(ActivityPredictor *activity_predictor, float *accel, float act_level,
                                           StepCounter *step_counter);
int simple_predict(ActivityPredictor *activity_predictor);

#endif

//
// Created by Darius on 11.01.2017.
//

#ifndef __PREDICTIONS_H__
#define __PREDICTIONS_H__

#define STAND_SIT 0
#define WALKING 1
#define LYING 2
#define WRONG_ORIENTATION 3

#include <stdbool.h>

int simple_predict();
void update_activity_classification_buffer(float *accel, float act_level);
bool get_is_buffer_full();

#endif

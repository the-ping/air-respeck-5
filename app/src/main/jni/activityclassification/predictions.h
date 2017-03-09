//
// Created by Darius on 11.01.2017.
//

#ifndef __PREDICTIONS_H__
#define __PREDICTIONS_H__

#include <stdbool.h>

int simple_predict();
void update_activity_classification_buffer(double *accel, double act_level);
bool get_is_buffer_full();

#endif

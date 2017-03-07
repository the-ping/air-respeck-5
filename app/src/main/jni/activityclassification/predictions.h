//
// Created by Darius on 11.01.2017.
//

#ifndef CONTINUOUSNEW_PREDICTIONS_H
#define CONTINUOUSNEW_PREDICTIONS_H

/* Here we store the previous 50 acceleration vectors and the maximum activity */
static const int ACT_CLASS_BUFFER_SIZE = 50;
static double act_class_buffer[ACT_CLASS_BUFFER_SIZE][4];
static int current_idx_in_buffer = 0;
static int is_buffer_full = 0;

int simple_predict();

void update_activity_classification_buffer(double *accel, double act_level);

int get_is_buffer_full();

#endif //CONTINUOUSNEW_PREDICTIONS_H

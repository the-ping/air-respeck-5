#include "predictions.h"
#include "../stepcount/step_count.h"
#include <memory.h>
//#include <android/log.h>

void initialise_activity_classification(ActivityPredictor *activity_predictor) {
    activity_predictor->last_prediction = -1;

    activity_predictor->current_idx_in_buffer = -1;
    activity_predictor->is_buffer_full = 0;
    activity_predictor->stepcount_is_walking = 0;
}

void update_activity_classification_buffer(ActivityPredictor *activity_predictor, float *accel, float act_level,
                                           StepCounter *step_counter) {
    activity_predictor->current_idx_in_buffer = (activity_predictor->current_idx_in_buffer + 1) % ACT_CLASS_BUFFER_SIZE;

    activity_predictor->act_class_buffer[activity_predictor->current_idx_in_buffer][0] = accel[1];
    activity_predictor->act_class_buffer[activity_predictor->current_idx_in_buffer][1] = act_level;

    if (!activity_predictor->is_buffer_full && activity_predictor->current_idx_in_buffer == ACT_CLASS_BUFFER_SIZE - 1) {
        activity_predictor->is_buffer_full = 1;
    }

    // Get information about movement from step count
    activity_predictor->stepcount_is_walking = is_walking(step_counter);
}

/* Quicksort. Needed for calculating the median */
int partition(float a[], int l, int r) {
    float pivot, tmp;
    int i, j;
    pivot = a[l];
    i = l;
    j = r + 1;

    while (1) {
        do ++i; while (a[i] <= pivot && i <= r);
        do --j; while (a[j] > pivot);
        if (i >= j) break;
        tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }
    tmp = a[l];
    a[l] = a[j];
    a[j] = tmp;
    return j;
}

void quick_sort(float a[], int l, int r) {
    int j;

    if (l < r) {
        // divide and conquer
        j = partition(a, l, r);
        quick_sort(a, l, j - 1);
        quick_sort(a, j + 1, r);
    }
}

float calc_median(const float data[], const int size) {

    float data_copy[size];

    memcpy(data_copy, data, size * sizeof(float));

    // sort the copy
    quick_sort(data_copy, 0, size - 1);

    if (size % 2 == 0) {
        // if there is an even number of elements, return mean of the two elements in the middle
        return (float) ((data_copy[size / 2] + data_copy[size / 2 - 1]) / 2.0);
    } else {
        // else return the element in the middle
        return data_copy[size / 2];
    }
}

int simple_predict(ActivityPredictor *activity_predictor) {
    // If the prediction buffer isn't filled yet, we cannot make any prediction. This should be checked
    // before calling this method, so return NULL in that case
    if (!activity_predictor->is_buffer_full) {
        return -1;
    }

    float ys[ACT_CLASS_BUFFER_SIZE], act_levels[ACT_CLASS_BUFFER_SIZE];
    /* Fill in the arrays of the past X acceleration values and maximum activity level */
    for (int buffer_idx = 0; buffer_idx < ACT_CLASS_BUFFER_SIZE; buffer_idx++) {
        ys[buffer_idx] = activity_predictor->act_class_buffer[buffer_idx][0];
        act_levels[buffer_idx] = activity_predictor->act_class_buffer[buffer_idx][1];
    }

    float y_median = calc_median(ys, ACT_CLASS_BUFFER_SIZE);
    // A median activity level greater than 0.025 indicates walking
    float al_median = calc_median(act_levels, ACT_CLASS_BUFFER_SIZE);

    // First, check whether we have an invalid orientation.
    // ==================

    // Is the sensor currently turned by 180° on the y-axis (vertical) above an angle which cannot occur while lying
    // down? This position can never occur except for a hand stand which we don't expect from the subjects.
    if (0.8 <= ys[activity_predictor->current_idx_in_buffer]) {
        activity_predictor->last_prediction = ACTIVITY_WRONG_ORIENTATION;
    } else if (-0.4 <= y_median) {
        /* Is y_median higher than -0.4?. If yes, we are lying down. Else, check activity levels.
         * -0.4 corresponds to an angle of ~23° from the ground (degrees(arccos(-0.4)))
         * If the activity level is above a certain threshold, we assume the
         * orientation is wrong, as walking during lying is not possible. Note that we use a considerably
         * higher activity level threshold than below as high activity values during posture changes are common
         * in lying position */
        if (al_median >= 0.11) {
            activity_predictor->last_prediction = ACTIVITY_WRONG_ORIENTATION;
        } else {
            activity_predictor->last_prediction = ACTIVITY_LYING;
        }
    } else {
        // If the last prediction was lying down, we don't want to predict walking if the person is getting up.
        // We circumvent that by clearing the max_act_level level buffer. Walking can only be predicted when it is filled
        // again with high enough values
        if (activity_predictor->last_prediction == ACTIVITY_LYING) {
            //__android_log_print(ANDROID_LOG_INFO, "DF",
            //                    "switched from lying do sit/stand -> clear activity level buffer!");
            for (int buffer_idx = 0; buffer_idx < ACT_CLASS_BUFFER_SIZE; buffer_idx++) {
                activity_predictor->act_class_buffer[buffer_idx][1] = 0;
            }
            activity_predictor->last_prediction = ACTIVITY_STAND_SIT;
        } else {
            //__android_log_print(ANDROID_LOG_INFO, "DF", "al median: %lf", al_median);
            if (activity_predictor->stepcount_is_walking) {
                activity_predictor->last_prediction = ACTIVITY_WALKING;
            } else if (al_median >= 0.043) {
                activity_predictor->last_prediction = ACTIVITY_MOVEMENT;
            } else {
                activity_predictor->last_prediction = ACTIVITY_STAND_SIT;
            }
        }
    }

    //__android_log_print(ANDROID_LOG_INFO, "DF", "%d", activity_predictor->last_prediction);

    return activity_predictor->last_prediction;
}



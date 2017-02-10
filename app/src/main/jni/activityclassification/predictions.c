//
// Created by Darius on 11.01.2017.
//

#include "predictions.h"
#include <android/log.h>
#include <memory.h>

static int last_prediction = -1;

void update_act_class_buffer(double *accel, double max_act_level) {
    double buffer_entry[4] = {accel[0], accel[1], accel[2], max_act_level};
    memcpy(act_class_buffer[current_idx_in_buffer], buffer_entry, 4 * sizeof(double));
    if (!is_buffer_full && current_idx_in_buffer == ACT_CLASS_BUFFER_SIZE - 1) {
        __android_log_print(ANDROID_LOG_INFO, "DF", "Buffer is full!");
        is_buffer_full = 1;
    }
    current_idx_in_buffer = (current_idx_in_buffer + 1) % ACT_CLASS_BUFFER_SIZE;
}

int get_is_buffer_full() {
    return is_buffer_full;
}

/* Quicksort. Needed for calculating the median */
int partition(double a[], int l, int r) {
    double pivot, tmp;
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

void quick_sort(double a[], int l, int r) {
    int j;

    if (l < r) {
        // divide and conquer
        j = partition(a, l, r);
        quick_sort(a, l, j - 1);
        quick_sort(a, j + 1, r);
    }
}

double calc_median(const double data[], const int size) {

    double data_copy[size];

    memcpy(data_copy, data, size * sizeof(double));

    /* sort the copy */
    quick_sort(data_copy, 0, size - 1);

    if (size % 2 == 0) {
        // if there is an even number of elements, return mean of the two elements in the middle
        return ((data_copy[size / 2] + data_copy[size / 2 - 1]) / 2.0);
    } else {
        // else return the element in the middle
        return data_copy[size / 2];
    }
}

int simple_predict() {
    /* If the prediction buffer isn't filled yet, we cannot make any prediction. This should be checked
     * before calling this method, so return NULL in that case */
    if (!is_buffer_full) {
        return -1;
    }

    double xs[ACT_CLASS_BUFFER_SIZE], ys[ACT_CLASS_BUFFER_SIZE], zs[ACT_CLASS_BUFFER_SIZE],
            max_act_levels[ACT_CLASS_BUFFER_SIZE];
    /* Fill in the arrays of the past X acceleration values and maximum activity level */
    for (int buffer_idx = 0; buffer_idx < ACT_CLASS_BUFFER_SIZE; buffer_idx++) {
        xs[buffer_idx] = act_class_buffer[buffer_idx][0];
        ys[buffer_idx] = act_class_buffer[buffer_idx][1];
        zs[buffer_idx] = act_class_buffer[buffer_idx][2];
        max_act_levels[buffer_idx] = act_class_buffer[buffer_idx][3];
    }

    double y_median = calc_median(ys, ACT_CLASS_BUFFER_SIZE);
    __android_log_print(ANDROID_LOG_INFO, "DF", "y median: %lf", y_median);

    // Is y value between -0.5 and 0.5?. If yes, we are lying down. Else, check activity levels
    //if ((-0.5 <= y_median) && (y_median <= 0.5)) { // an angle of > 30° counts as sitting
    if (-0.4 <= y_median) {
        last_prediction = 2; // Lying down
    } else {
        // If the last prediction was lying down, we don't want to predict walking if the person is getting up.
        // We circumvent that by clearing the activity level buffer. Walking can only be predicted when it is filled
        // again with high enough values
        if (last_prediction == 2) {
            __android_log_print(ANDROID_LOG_INFO, "DF",
                                "switched from lying do sit/stand -> clear activity level buffer!");
            for (int buffer_idx = 0; buffer_idx < ACT_CLASS_BUFFER_SIZE; buffer_idx++) {
                act_class_buffer[buffer_idx][3] = 0;
            }
            last_prediction = 0; // Predict sitting
        } else {
            double al_median = calc_median(max_act_levels, ACT_CLASS_BUFFER_SIZE);
            __android_log_print(ANDROID_LOG_INFO, "DF", "al median: %lf", al_median);
            if (al_median >= 0.025) { // Determined with distribution of activity levels with 5 subjects
                last_prediction = 1; // Walking
            } else {
                last_prediction = 0; // Sitting/standing
            }
        }
    }
    return last_prediction;
}



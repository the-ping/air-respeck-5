#include "predictions.h"
#include "../stepcount/step_count.h"
#include <memory.h>
//#include <android/log.h>

void initialise_activity_classification(ActivityPredictor *activity_predictor) {
    activity_predictor->last_prediction = -1;
}

void update_activity_classification(ActivityPredictor *activity_predictor, float *accel, StepCounter *step_counter) {
    activity_predictor->last_prediction = get_advanced_activity_prediction(accel[0], accel[1], accel[2],
                                                                           is_walking(step_counter),
                                                                           is_moving(step_counter));
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


int get_advanced_activity_prediction(float x, float y, float z, bool is_walking, bool is_moving) {
    // Sensor turned by 180°?
    if (y > 0.8) {
        return ACTIVITY_WRONG_ORIENTATION;
    }

    if (is_walking) {
        return ACTIVITY_WALKING;
    }
    if (is_moving) {
        return ACTIVITY_MOVEMENT;
    }

    // Lying on side?
    if (x < -0.66129816) {
        return ACTIVITY_LYING_DOWN_RIGHT;  // Lying down to the right
    } else if (x > 0.67874145) {
        return ACTIVITY_LYING_DOWN_LEFT;  // Lying down to the left
    } else if (y > -0.28865455) {
        // Lying either on back or stomach:
        if (z > 0) {
            return ACTIVITY_LYING; // Lying down normal on back
        } else {
            return ACTIVITY_LYING_DOWN_STOMACH; // Lying down on stomach
        }
    } else {
        // Not lying
        // Leaning forward or backward? -> Sitting
        if (z > 0.43) {
            return ACTIVITY_SITTING_BENT_BACKWARD; // Sitting bent backward
        } else if (z < -0.43) {
            return ACTIVITY_SITTING_BENT_FORWARD; // Sitting bent forward
        }

        // Default to sitting/standing
        return ACTIVITY_STAND_SIT;
    }
}



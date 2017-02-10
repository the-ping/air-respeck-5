#include "activity_filter.h"

#include <math.h>

void ACT_init(activity_filter *filter) {
    filter->fill = 0;
    filter->pos = 0;
    filter->valid = false;
    filter->prev_accel_valid = false;
}

void ACT_update(double accel[3], activity_filter *filter) {

    if (isnan(accel[0]) || isnan(accel[1]) || isnan(accel[2])) {
        filter->valid = false;
        return;
    }

    if (filter->prev_accel_valid == false) {

        filter->prev_accel[0] = accel[0];
        filter->prev_accel[1] = accel[1];
        filter->prev_accel[2] = accel[2];

        filter->prev_accel_valid = true;

        filter->valid = false;

        return;

    }

    double f;
    f = sqrt((accel[0] - filter->prev_accel[0]) * (accel[0] - filter->prev_accel[0]) +
             (accel[1] - filter->prev_accel[1]) * (accel[1] - filter->prev_accel[1]) +
             (accel[2] - filter->prev_accel[2]) * (accel[2] - filter->prev_accel[2]));
    filter->values[filter->pos] = f;
    filter->pos = (filter->pos + 1) % ACTIVITY_BUFFER_SIZE;

    if (filter->fill < ACTIVITY_BUFFER_SIZE) {
        filter->fill++;
    }

    if (filter->fill < ACTIVITY_BUFFER_SIZE) {
        filter->valid = false;
        return;
    }

    filter->max = filter->values[0];

    for (int i = 0; i < ACTIVITY_BUFFER_SIZE; i++) {
        if (filter->values[i] > filter->max) {
            filter->max = filter->values[i];
        }
    }

    filter->prev_accel[0] = accel[0];
    filter->prev_accel[1] = accel[1];
    filter->prev_accel[2] = accel[2];

    filter->valid = true;
}

#include "mean_buffer_3d.h"
#include "math_helper.h"

void initialise_mean_buffer_3d(MeanBuffer3D *mean_buffer_3d) {
    mean_buffer_3d->fill = 0;
    mean_buffer_3d->current_position = 0;
    mean_buffer_3d->is_valid = false;

    mean_buffer_3d->sum[0] = 0;
    mean_buffer_3d->sum[1] = 0;
    mean_buffer_3d->sum[2] = 0;

    for (int i = 0; i < MEAN_AVERAGE_SIZE; i++) {
        mean_buffer_3d->values[i][0] = 0;
        mean_buffer_3d->values[i][1] = 0;
        mean_buffer_3d->values[i][2] = 0;
    }
}

void update_mean_buffer_3d(double *new_accel_data, MeanBuffer3D *mean_buffer_3d) {

    mean_buffer_3d->sum[0] -= mean_buffer_3d->values[mean_buffer_3d->current_position][0];
    mean_buffer_3d->sum[1] -= mean_buffer_3d->values[mean_buffer_3d->current_position][1];
    mean_buffer_3d->sum[2] -= mean_buffer_3d->values[mean_buffer_3d->current_position][2];

    copy_accel_vector(mean_buffer_3d->values[mean_buffer_3d->current_position], new_accel_data);

    mean_buffer_3d->sum[0] += mean_buffer_3d->values[mean_buffer_3d->current_position][0];
    mean_buffer_3d->sum[1] += mean_buffer_3d->values[mean_buffer_3d->current_position][1];
    mean_buffer_3d->sum[2] += mean_buffer_3d->values[mean_buffer_3d->current_position][2];

    mean_buffer_3d->current_position = (mean_buffer_3d->current_position + 1) % MEAN_AVERAGE_SIZE;

    if (mean_buffer_3d->fill < MEAN_AVERAGE_SIZE) {
        mean_buffer_3d->fill++;
    }

    if (mean_buffer_3d->fill < MEAN_AVERAGE_SIZE) {
        mean_buffer_3d->is_valid = false;
        return;
    }

    mean_buffer_3d->mean_values[0] = mean_buffer_3d->sum[0] / MEAN_AVERAGE_SIZE;
    mean_buffer_3d->mean_values[1] = mean_buffer_3d->sum[1] / MEAN_AVERAGE_SIZE;
    mean_buffer_3d->mean_values[2] = mean_buffer_3d->sum[2] / MEAN_AVERAGE_SIZE;

    mean_buffer_3d->is_valid = true;
}

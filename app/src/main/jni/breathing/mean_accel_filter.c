#include "mean_accel_filter.h"
#include "math_helper.h"

void initialise_mean_accel_filter(MeanAccelFilter *mean_accel_filter) {
    mean_accel_filter->fill = 0;
    mean_accel_filter->current_position = 0;
    mean_accel_filter->is_valid = false;

    mean_accel_filter->sum[0] = 0;
    mean_accel_filter->sum[1] = 0;
    mean_accel_filter->sum[2] = 0;

    for (int i = 0; i < MEAN_ACCEL_FILTER_SIZE; i++) {
        mean_accel_filter->values[i][0] = 0;
        mean_accel_filter->values[i][1] = 0;
        mean_accel_filter->values[i][2] = 0;
    }
}

void update_mean_accel_filter(double *new_accel_data, MeanAccelFilter *mean_accel_filter) {

    subtract_from_accel_vector(mean_accel_filter->sum, mean_accel_filter->values[mean_accel_filter->current_position]);
    copy_accel_vector(mean_accel_filter->values[mean_accel_filter->current_position], new_accel_data);
    add_to_accel_vector(mean_accel_filter->sum, mean_accel_filter->values[mean_accel_filter->current_position]);

    mean_accel_filter->current_position = (mean_accel_filter->current_position + 1) % MEAN_ACCEL_FILTER_SIZE;

    if (mean_accel_filter->fill < MEAN_ACCEL_FILTER_SIZE) {
        mean_accel_filter->fill++;
    }

    if (mean_accel_filter->fill < MEAN_ACCEL_FILTER_SIZE) {
        mean_accel_filter->is_valid = false;
        return;
    }

    mean_accel_filter->mean_accel_values[0] = mean_accel_filter->sum[0] / MEAN_ACCEL_FILTER_SIZE;
    mean_accel_filter->mean_accel_values[1] = mean_accel_filter->sum[1] / MEAN_ACCEL_FILTER_SIZE;
    mean_accel_filter->mean_accel_values[2] = mean_accel_filter->sum[2] / MEAN_ACCEL_FILTER_SIZE;

    mean_accel_filter->is_valid = true;
}

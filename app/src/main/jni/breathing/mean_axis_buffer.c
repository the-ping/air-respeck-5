#include "mean_axis_buffer.h"

#include "math_helper.h"
//#include "arm_math.h"

void get_reference_axis(double *ref);

void initialise_mean_axis_buffer(MeanAxisBuffer *mean_axis_buffer) {

    mean_axis_buffer->fill = 0;
    mean_axis_buffer->pos = 0;
    mean_axis_buffer->is_valid = 0;

    mean_axis_buffer->sum[0] = 0;
    mean_axis_buffer->sum[1] = 0;
    mean_axis_buffer->sum[2] = 0;

    int i;
    for (i = 0; i < MEAN_AXIS_SIZE; i++) {
        mean_axis_buffer->accel_data[i][0] = 0;
        mean_axis_buffer->accel_data[i][1] = 0;
        mean_axis_buffer->accel_data[i][2] = 0;
    }

}

void update_mean_axis_buffer(double *new_accel_data, MeanAxisBuffer *mean_axis_buffer) {

    double reference_axis[3], dot_res;
    get_reference_axis(reference_axis);

    mean_axis_buffer->sum[0] -= mean_axis_buffer->accel_data[mean_axis_buffer->pos][0];
    mean_axis_buffer->sum[1] -= mean_axis_buffer->accel_data[mean_axis_buffer->pos][1];
    mean_axis_buffer->sum[2] -= mean_axis_buffer->accel_data[mean_axis_buffer->pos][2];

    copy_accel_vector(mean_axis_buffer->accel_data[mean_axis_buffer->pos], new_accel_data);

    // Compare acceleration data to reference axis. If negative, invert acceleration data!
    dot_res = dot_product(new_accel_data, reference_axis);

    if (dot_res < 0) {
        mean_axis_buffer->accel_data[mean_axis_buffer->pos][0] *= -1.0;
        mean_axis_buffer->accel_data[mean_axis_buffer->pos][1] *= -1.0;
        mean_axis_buffer->accel_data[mean_axis_buffer->pos][2] *= -1.0;
    }

    mean_axis_buffer->sum[0] += mean_axis_buffer->accel_data[mean_axis_buffer->pos][0];
    mean_axis_buffer->sum[1] += mean_axis_buffer->accel_data[mean_axis_buffer->pos][1];
    mean_axis_buffer->sum[2] += mean_axis_buffer->accel_data[mean_axis_buffer->pos][2];

    mean_axis_buffer->pos = (mean_axis_buffer->pos + 1) % MEAN_AXIS_SIZE;

    if (mean_axis_buffer->fill < MEAN_AXIS_SIZE) {
        mean_axis_buffer->fill++;
    }

    if (mean_axis_buffer->fill < MEAN_AXIS_SIZE) {
        mean_axis_buffer->is_valid = false;
        return;
    }

    mean_axis_buffer->value[0] = mean_axis_buffer->sum[0];
    mean_axis_buffer->value[1] = mean_axis_buffer->sum[1];
    mean_axis_buffer->value[2] = mean_axis_buffer->sum[2];

    normalise_vector_to_unit_length(mean_axis_buffer->value);
    mean_axis_buffer->is_valid = true;
}

// TODO: why do we use a fixed reference axis??
void get_reference_axis(double *ref) {

    ref[0] = 0.98499424;
    ref[1] = -0.17221591;
    ref[2] = 0.01131468;

}

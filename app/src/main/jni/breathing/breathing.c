#include "breathing.h"

#include <math.h>
#include <android/log.h>

#include "mean_buffer_3d.h"
#include "rotation_axis.h"
#include "mean_buffer.h"
#include "mean_axis_buffer.h"
#include "mean_unit_accel_buffer.h"
#include "activity_level_buffer.h"
#include "../activityclassification/predictions.h"

MeanBuffer3D mean_accel_buffer;
RotationAxisBuffer rotation_axis_buffer;
MeanAxisBuffer mean_axis_buffer;
MeanUnitAccelBuffer mean_unit_accel_buffer;
MeanBuffer mean_buffer_breathing_signal;
MeanBuffer mean_buffer_angle;
ActivityLevelBuffer activity_level_buffer;

void initialise_breathing_buffer(BreathingBuffer *breathing_buffer) {
    breathing_buffer->is_breathing_initialised = false;
    breathing_buffer->is_valid = false;
    breathing_buffer->signal = NAN;
    breathing_buffer->angle = NAN;
    breathing_buffer->max_act_level = NAN;

    initialise_mean_buffer_3d(&mean_accel_buffer);
    initialise_rotation_axis_buffer(&rotation_axis_buffer);
    initialise_mean_axis_buffer(&mean_axis_buffer);
    initialise_mean_unit_accel_buffer(&mean_unit_accel_buffer);
    initialise_mean_buffer(&mean_buffer_breathing_signal);
    initialise_mean_buffer(&mean_buffer_angle);
    initialise_activity_level_buffer(&activity_level_buffer);
}

void update_breathing(double *new_accel_data, BreathingBuffer *breathing_buffer) {
    if (breathing_buffer->is_breathing_initialised == false) {
        breathing_buffer->is_valid = false;
        return;
    }

    // Return if any of the acceleration values are NAN. This shouldn't happen.
    if (isnan(new_accel_data[0]) || isnan(new_accel_data[1]) || isnan(new_accel_data[2])) {
        breathing_buffer->is_valid = false;
        return;
    }

    double copy_of_new_accel_data[3];
    copy_accel_vector(copy_of_new_accel_data, new_accel_data);

    breathing_buffer->is_valid = false;

    int previous_position = activity_level_buffer.current_position;

    update_activity_level_buffer(copy_of_new_accel_data, &activity_level_buffer);

    // Update max_act_level buffer -> we want the actual max_act_level level, not the maximum
    update_activity_classification_buffer(copy_of_new_accel_data, activity_level_buffer.values[previous_position]);

    if (activity_level_buffer.is_valid == false) {
        breathing_buffer->is_valid = false;
        return;
    }

    breathing_buffer->max_act_level = activity_level_buffer.max;

    if (activity_level_buffer.max > ACTIVITY_CUTOFF) {
        breathing_buffer->is_valid = false;
        breathing_buffer->signal = NAN;
        return;
    }

    update_mean_buffer_3d(copy_of_new_accel_data, &mean_accel_buffer);

    if (mean_accel_buffer.is_valid == false) {
        breathing_buffer->is_valid = false;
        return;
    }

    copy_accel_vector(copy_of_new_accel_data, mean_accel_buffer.mean_values);
    normalise_vector_to_unit_length(copy_of_new_accel_data);

    update_rotation_axis_buffer(copy_of_new_accel_data, &rotation_axis_buffer);

    if (rotation_axis_buffer.is_current_axis_valid == false) {
        breathing_buffer->is_valid = false;
        return;
    }

    update_mean_unit_accel_buffer(copy_of_new_accel_data, &mean_unit_accel_buffer);

    update_mean_axis_buffer(rotation_axis_buffer.current_axis, &mean_axis_buffer);

    if (mean_axis_buffer.is_valid == false) {
        breathing_buffer->is_valid = false;
        return;
    }

    // Breathing signal calculation
    double final_bs = dot_product(rotation_axis_buffer.current_axis, mean_axis_buffer.value);
    __android_log_print(ANDROID_LOG_INFO, "BS", "bs: %lf", final_bs);
    // TODO: this should be completely unnecessary, as the amplitude of the breathing signal ins meaningless anyway
    final_bs = final_bs * SAMPLE_RATE * 10.0f;

    // Breathing angle calculation
    double mean_accel_cross_mean_axis[3];
    cross_product(mean_accel_cross_mean_axis, mean_unit_accel_buffer.mean_unit_vector, mean_axis_buffer.value);
    double final_ba;
    final_ba = dot_product(mean_accel_cross_mean_axis, copy_of_new_accel_data);

    update_mean_buffer(final_bs, &mean_buffer_breathing_signal);
    update_mean_buffer(final_ba, &mean_buffer_angle);

    if (mean_buffer_breathing_signal.is_valid == false) {
        breathing_buffer->is_valid = false;
        return;
    }

    // update the breathing signal and breathing angle
    breathing_buffer->signal = mean_buffer_breathing_signal.value;
    breathing_buffer->angle = mean_buffer_angle.value;
    breathing_buffer->is_valid = true;
}

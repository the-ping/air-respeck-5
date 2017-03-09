#include "breathing.h"

#include <math.h>

#include "mean_accel_filter.h"
#include "rotation_axis.h"
#include "mean_buffer.h"
#include "mean_axis_buffer.h"
#include "mean_unit_accel_buffer.h"
#include "activity_level_buffer.h"
#include "../activityclassification/predictions.h"

MeanAccelFilter mean_accel_filter;
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

    initialise_mean_accel_filter(&mean_accel_filter);
    initialise_rotation_axis_buffer(&rotation_axis_buffer);
    initialise_mean_axis_buffer(&mean_axis_buffer);
    initialise_mean_unit_accel_buffer(&mean_unit_accel_buffer);
    initialise_mean_buffer(&mean_buffer_breathing_signal);
    initialise_mean_buffer(&mean_buffer_angle);
    initialise_activity_level_buffer(&activity_level_buffer);
}

void update_breathing(double *new_accel_data_original, BreathingBuffer *breathing_buffer) {
    // We assume apriori that the current breathing value is not valid.
    breathing_buffer->is_valid = false;

    if (breathing_buffer->is_breathing_initialised == false) {
        return;
    }

    // Return if any of the acceleration values are NAN. This shouldn't happen.
    if (isnan(new_accel_data_original[0]) || isnan(new_accel_data_original[1]) || isnan(new_accel_data_original[2])) {
        return;
    }

    // First, make a copy of input accel vector and use that in the following
    double new_accel_data[3];
    copy_accel_vector(new_accel_data, new_accel_data_original);

    // Save the position where the next activity level will be saved. This is the current position in the buffer.
    int position_of_next_activity_level = activity_level_buffer.current_position;

    // Update the activity level buffer. The current position will one position after the most recent activity level
    // after the update.
    update_activity_level_buffer(new_accel_data, &activity_level_buffer);

    // With the previously saved position, we can now access the most recent activity level and save it in the
    // classification buffer.
    update_activity_classification_buffer(new_accel_data,
                                          activity_level_buffer.activity_levels[position_of_next_activity_level]);

    // Wait until the activity level buffer is filled before continuing with the breathing signal calculations
    if (activity_level_buffer.is_valid == false) {
        return;
    }

    // Use the maximum activity level in the buffer as a threshold to determine movement
    breathing_buffer->max_act_level = activity_level_buffer.max;
    if (activity_level_buffer.max > ACTIVITY_CUTOFF) {
        breathing_buffer->signal = NAN;
        return;
    }

    // Fill up mean filter buffer. This smooths the acceleration values by returning the mean for each window of
    // MEAN_ACCEL_FILTER_SIZE samples
    update_mean_accel_filter(new_accel_data, &mean_accel_filter);

    if (mean_accel_filter.is_valid == false) {
        return;
    }

    // Get the mean acceleration vector as soon as the filter is full and normalise it.
    copy_accel_vector(new_accel_data, mean_accel_filter.mean_accel_values);
    normalise_vector_to_unit_length(new_accel_data);

    // Determine rotation axis
    update_rotation_axis_buffer(new_accel_data, &rotation_axis_buffer);

    if (rotation_axis_buffer.is_current_axis_valid == false) {
        return;
    }

    // Another mean acceleration vector with unit length
    update_mean_unit_accel_buffer(new_accel_data, &mean_unit_accel_buffer);

    // Mean rotation axis
    update_mean_axis_buffer(rotation_axis_buffer.current_axis, &mean_axis_buffer);

    if (mean_axis_buffer.is_valid == false) {
        return;
    }

    // Breathing signal calculation
    double final_bs = dot_product(rotation_axis_buffer.current_axis, mean_axis_buffer.mean_axis);
//    __android_log_print(ANDROID_LOG_INFO, "BS", "bs: %lf", final_bs);
    // TODO: this should be completely unnecessary, as the amplitude of the breathing signal ins meaningless anyway
    final_bs = final_bs * SAMPLE_RATE * 10.0f;

    // Breathing angle calculation
    double mean_accel_cross_mean_axis[3];
    cross_product(mean_accel_cross_mean_axis, mean_unit_accel_buffer.mean_unit_vector, mean_axis_buffer.mean_axis);
    double final_ba;
    final_ba = dot_product(mean_accel_cross_mean_axis, new_accel_data);

    // Smooth the breathing signal and angles for the last time
    update_mean_buffer(final_bs, &mean_buffer_breathing_signal);
    update_mean_buffer(final_ba, &mean_buffer_angle);

    if (mean_buffer_breathing_signal.is_valid == false) {
        return;
    }

    // update the breathing signal and breathing angle
    breathing_buffer->signal = mean_buffer_breathing_signal.value;
    breathing_buffer->angle = mean_buffer_angle.value;

    // Only if we made it to the end do we have a valid breathing signal
    breathing_buffer->is_valid = true;
}

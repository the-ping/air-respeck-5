/*
 * Main class with the algorithm to calculate the breathing signal and other measures.
 * Most other classes in the breathing directory are used for calculating the mean of some data. Whenever
 * this mean is used for smoothing the signal, the struct and variable is called "filter". When the
 * mean is used for other purposes, such as getting a "reference axis", it is called "buffer".
 */
#include "breathing.h"

#include <math.h>
#include <malloc.h>

#include "rotation_axis.h"
#include "mean_post_filter.h"
#include "mean_axis_buffer.h"
#include "mean_unit_accel_buffer.h"
#include "activity_level_buffer.h"
#include "../activityclassification/predictions.h"
#include "../stepcount/step_count.h"

static MeanUnitAccelBuffer mean_unit_accel_filter;
static RotationAxis rotation_axis;
static MeanRotationAxisBuffer mean_rotation_axis_buffer;
static MeanUnitAccelBuffer mean_unit_accel_buffer;
static MeanPostFilter mean_filter_breathing_signal;
static MeanPostFilter mean_filter_angle;
static ActivityLevelBuffer activity_level_buffer;


void initialise_breathing_measures(BreathingMeasures *breathing_measures, unsigned int pre_filter_length,
                                   unsigned int post_filter_length, float activity_cutoff) {
    breathing_measures->is_breathing_initialised = false;
    breathing_measures->is_valid = false;
    breathing_measures->signal = NAN;
    breathing_measures->angle = NAN;
    breathing_measures->max_act_level = NAN;
    breathing_measures->activity_cutoff = activity_cutoff;

    initialise_mean_unit_accel_buffer(&mean_unit_accel_filter, pre_filter_length);
    initialise_rotation_axis(&rotation_axis);
    initialise_mean_rotation_axis_buffer(&mean_rotation_axis_buffer);
    initialise_mean_unit_accel_buffer(&mean_unit_accel_buffer, 128);
    initialise_mean_post_filter(&mean_filter_breathing_signal, post_filter_length);
    initialise_mean_post_filter(&mean_filter_angle, post_filter_length);
    initialise_activity_level_buffer(&activity_level_buffer);
}

void update_breathing_measures(float *new_accel_data_original, BreathingMeasures *breathing_measures,
                               StepCounter *step_counter, ActivityPredictor *activity_predictor) {
    // We assume apriori that the current breathing value is not valid.
    breathing_measures->is_valid = false;

    if (breathing_measures->is_breathing_initialised == false) {
        return;
    }

    // Return if any of the acceleration values are NAN. This shouldn't happen.
    if (isnan(new_accel_data_original[0]) || isnan(new_accel_data_original[1]) ||
        isnan(new_accel_data_original[2])) {
        return;
    }

    // First, make a copy of input accel vector and use that in the following
    float *new_accel_data = calloc(3, sizeof(float));
    copy_accel_vector(new_accel_data, new_accel_data_original);

    // Fill the buffer for the activity level
    update_activity_level_buffer(new_accel_data, &activity_level_buffer);

    update_activity_classification(activity_predictor, new_accel_data, step_counter);

    // Wait until the activity level buffer is filled before continuing with the breathing signal calculations
    if (activity_level_buffer.is_valid == false) {
        return;
    }

    // Use the maximum activity level in the buffer as a threshold to determine movement
    breathing_measures->max_act_level = activity_level_buffer.max;
    if (activity_level_buffer.max > breathing_measures->activity_cutoff) {
        breathing_measures->signal = NAN;
        return;
    }

    // If the subject is currently walking, discard signal
    /* TODO: only uncomment if step counter is reliable enough. Currently (02-2019), it isn't.
    if (is_walking(step_counter)) {
        breathing_measures->signal = NAN;
        return;
    }
     */

    // Fill up mean filter buffer. This smooths the acceleration values by returning the mean for each window of
    // 12 samples
    update_mean_unit_accel_buffer(new_accel_data, &mean_unit_accel_filter);

    if (mean_unit_accel_filter.is_valid == false) {
        return;
    }

    // Get the mean acceleration vector as soon as the filter is full and normalise it. We then overwrite the
    // new acceleration values with this value, i.e. this is actually used for smoothing.
    copy_accel_vector(new_accel_data, mean_unit_accel_filter.mean_unit_vector);

    // Determine rotation axis
    update_rotation_axis(new_accel_data, &rotation_axis);

    if (rotation_axis.is_current_axis_valid == false) {
        return;
    }

    // Another mean acceleration vector with unit length. This time, the new_accel_data is not overwritten with the
    // mean value.
    update_mean_unit_accel_buffer(new_accel_data, &mean_unit_accel_buffer);

    // Mean rotation axis
    update_mean_rotation_axis_buffer(rotation_axis.current_axis, &mean_rotation_axis_buffer);

    if (mean_rotation_axis_buffer.is_valid == false) {
        return;
    }

    // Breathing signal calculation. The current rotation axis is proportional to the current angle change.
    // With the dot product, this value is projected onto the mean rotation axis which makes the
    // angle changes comparable. This corresponds to the rotational change used in the Python code.
    float breathing_signal = dot_product(rotation_axis.current_axis, mean_rotation_axis_buffer.mean_axis);
    // TODO: Modify this factor so it corresponds as close as possible to the spirometer readings
    breathing_signal = (float) (breathing_signal * 120.);

    // Breathing angle calculation
    float mean_accel_cross_mean_axis[3];
    cross_product(mean_accel_cross_mean_axis, mean_unit_accel_buffer.mean_unit_vector,
                  mean_rotation_axis_buffer.mean_axis);
    float breathing_angle;
    breathing_angle = dot_product(mean_accel_cross_mean_axis, new_accel_data);

    // Smooth the breathing signal and angles for the last time
    update_mean_post_filter(breathing_signal, &mean_filter_breathing_signal);
    update_mean_post_filter(breathing_angle, &mean_filter_angle);

    if (mean_filter_breathing_signal.is_valid == false) {
        return;
    }

    // update the breathing signal and breathing angle
    breathing_measures->signal = mean_filter_breathing_signal.mean_value;
    breathing_measures->angle = mean_filter_angle.mean_value;

    // Only if we made it to the end do we have a valid breathing signal
    breathing_measures->is_valid = true;
}

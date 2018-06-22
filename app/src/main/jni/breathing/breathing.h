#ifndef __BREATHING_H__
#define __BREATHING_H__

#define SAMPLE_RATE 12.5

#include <stdint.h>
#include <stdbool.h>
#include "../stepcount/step_count.h"
#include "../activityclassification/predictions.h"

typedef struct {
    float signal;
    float angle;
    bool is_valid;
    float max_act_level;
    bool is_breathing_initialised;
    float activity_cutoff;

} BreathingMeasures;

void initialise_breathing_measures(BreathingMeasures *breathing_measures, unsigned int pre_filter_length,
                                   unsigned int post_filter_length, float activity_cutoff);

void update_breathing_measures(float *new_accel_data_original, BreathingMeasures *breathing_measures,
                               StepCounter *step_counter, ActivityPredictor *activityPredictor);

#endif

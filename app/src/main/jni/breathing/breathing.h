#ifndef __BREATHING_H__
#define __BREATHING_H__

#define SAMPLE_RATE 12.5

#include <stdint.h>
#include <stdbool.h>

typedef struct {
    float signal;
    float angle;
    bool is_valid;
    float max_act_level;
    bool is_breathing_initialised;
    float activity_cutoff;

} BreathingMeasures;

void initialise_breathing_measures(BreathingMeasures *breathing_measures, bool isPostFilteringEnabled,
                                   float activity_cutoff);

void update_breathing_measures(float *new_accel_data_original, BreathingMeasures *breathing_measures);

#endif

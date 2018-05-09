package com.specknet.airrespeck.activities;

import com.specknet.airrespeck.models.PulseoxData;

/**
 * Created by Darius on 24.05.2017.
 */

public interface PulseoxDataObserver {
    void updatePulseoxData(PulseoxData data);
}

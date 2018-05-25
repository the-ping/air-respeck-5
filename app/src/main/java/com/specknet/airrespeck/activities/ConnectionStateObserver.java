package com.specknet.airrespeck.activities;

import com.specknet.airrespeck.models.AirspeckData;

/**
 * Created by Darius on 24.05.2017.
 */

public interface ConnectionStateObserver {
    void updateConnectionState(boolean respeckConnected, boolean airspeckConnected, boolean pulseoxConnected);
}

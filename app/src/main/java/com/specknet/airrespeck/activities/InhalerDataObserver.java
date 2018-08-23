package com.specknet.airrespeck.activities;

import com.specknet.airrespeck.models.InhalerData;

/**
 * Created by Darius on 24.05.2017.
 */

public interface InhalerDataObserver {
    void updateInhalerData(InhalerData data);
}
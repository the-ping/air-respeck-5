package com.specknet.airrespeck.activities;

import com.specknet.airrespeck.models.RESpeckLiveData;

/**
 * Created by Darius on 24.05.2017.
 */

public interface RESpeckDataObserver {
    void updateRESpeckData(RESpeckLiveData data);
}

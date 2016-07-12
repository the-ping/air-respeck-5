package com.specknet.airrespeck.models;


import android.graphics.Color;

import com.specknet.airrespeck.lib.Segment;

import java.util.ArrayList;
import java.util.List;


public class ReadingItem {
    public String name;
    public String unit;
    public float minVal;
    public float maxVal;
    public float value;

    // Only for Segmented Bar
    public List<Segment> segments = null;

    public ReadingItem(String name, String unit, float value) {
        this.name = name;
        this.unit = unit;
        this.value = value;
    }

    public ReadingItem(String name, String unit, float minVal, float maxVal, float value) {
        this.name = name;
        this.unit = unit;
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.value = value;
        this.segments = new ArrayList<Segment>();
        this.segments.add(new Segment(this.minVal, this.maxVal, "", Color.parseColor("#2baf2b")));
    }

    public ReadingItem(String name, String unit, float value, List<Segment> segments) {
        this.name = name;
        this.unit = unit;
        this.value = value;
        this.segments = segments;
    }

    @Override
    public String toString() {
        return name + " (" + unit + ") = " + String.valueOf(value);
    }
}

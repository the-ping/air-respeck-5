package com.specknet.airrespeck.models;


import android.graphics.Color;

import com.specknet.airrespeck.lib.Segment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class ReadingItem implements Serializable {
    public String name;
    public String unit;
    public float minVal;
    public float maxVal;
    public float value;
    public String stringValue;

    // Valid only for Segmented Bar
    public List<Segment> segments = null;

    // Valid only for Arc Progress
    public int colors[];
    public int bgColor = Color.parseColor("#9e9e9e");

    // Valid for both
    public int defaultColor = Color.parseColor("#2baf2b");

    public ReadingItem(String name, String unit, String value) {
        this.name = name;
        this.unit = unit;
        this.stringValue = value;
    }

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
        this.segments.add(new Segment(this.minVal, this.maxVal, "", defaultColor));
    }

    public ReadingItem(String name, String unit, float value, List<Segment> segments) {
        this.name = name;
        this.unit = unit;
        this.value = value;
        this.segments = segments;

        if (this.segments !=  null) {
            this.minVal = this.segments.get(0).getMinValue();
            this.maxVal = this.segments.get(this.segments.size()-1).getMaxValue();

            if (this.segments.size() >= 2) {
                this.colors = new int[this.segments.size()];
                for (int i = 0; i < this.segments.size(); ++i) {
                    this.colors[i] = this.segments.get(i).getColor();
                }
            }
        }
    }

    @Override
    public String toString() {
        return name + " (" + unit + ") = " + String.valueOf(value);
    }
}

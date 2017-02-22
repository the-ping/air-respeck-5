package com.specknet.airrespeck.models;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Naohiro on 2017/01/28.
 */
public class XAxisValueFormatter implements IAxisValueFormatter {

    private SimpleDateFormat dateFormat;

    public XAxisValueFormatter() {
        dateFormat = new SimpleDateFormat("mm:ss.SS", Locale.UK);
    }

    @Override
    public String getFormattedValue(float timestamp, AxisBase axis) {
        Date date = new Date((long) timestamp);
        return dateFormat.format(date);
    }
}

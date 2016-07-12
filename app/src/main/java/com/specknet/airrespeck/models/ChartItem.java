package com.specknet.airrespeck.models;

import android.content.Context;
import android.view.View;

import com.github.mikephil.charting.data.ChartData;


public abstract class ChartItem {
    
    protected static final int TYPE_BARCHART = 0;
    protected static final int TYPE_LINECHART = 1;

    protected ChartData<?> mChartData;
    
    public ChartItem(ChartData<?> cd) {
        this.mChartData = cd;      
    }
    
    public abstract int getItemType();
    
    public abstract View getView(int position, View convertView, Context c);
}

package com.specknet.airrespeck.adapters;


import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.lib.SegmentedBar;
import com.specknet.airrespeck.models.ReadingItem;

import java.util.List;


/**
 * {@link RecyclerView.Adapter} that can display a {@link ReadingItem}
 */
public class ReadingItemSegmentedBarAdapter extends
        RecyclerView.Adapter<ReadingItemSegmentedBarAdapter.ViewHolder> {

    private final List<ReadingItem> mValues;

    public ReadingItemSegmentedBarAdapter(Context context,
                                          List<ReadingItem> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_segmentedbar, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mName.setText(holder.mItem.name);
        holder.mSegmentedBar.setValueWithUnit(holder.mItem.value, holder.mItem.unit);
        holder.mSegmentedBar.setSegments(holder.mItem.segments);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mName;
        public SegmentedBar mSegmentedBar;
        public ReadingItem mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mName = (TextView) view.findViewById(R.id.name);
            mSegmentedBar = (SegmentedBar) view.findViewById(R.id.segmented_bar);
            //mSegmentedBar.setValueTextSize(80);
            //mSegmentedBar.setValueSignSize(300, 130);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mName.getText() + "'";
        }
    }
}

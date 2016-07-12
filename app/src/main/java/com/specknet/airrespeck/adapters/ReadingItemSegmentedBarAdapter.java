package com.specknet.airrespeck.adapters;


import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.fragments.AQReadingsFragment;
import com.specknet.airrespeck.lib.Segment;
import com.specknet.airrespeck.lib.SegmentedBar;
import com.specknet.airrespeck.lib.SegmentedBarSideStyle;
import com.specknet.airrespeck.models.ReadingItem;

import java.util.ArrayList;
import java.util.List;


/**
 * {@link RecyclerView.Adapter} that can display a {@link ReadingItem} and makes a call
 * to the specified {@link AQReadingsFragment.OnAQFragmentInteractionListener}.
 */
public class ReadingItemSegmentedBarAdapter extends
        RecyclerView.Adapter<ReadingItemSegmentedBarAdapter.ViewHolder> {

    private final Context mContext;
    private final List<ReadingItem> mValues;
    private final AQReadingsFragment.OnAQFragmentInteractionListener mListener;

    public ReadingItemSegmentedBarAdapter(Context context,
                                          List<ReadingItem> items,
                                          AQReadingsFragment.OnAQFragmentInteractionListener listener) {
        mContext = context;
        mValues = items;
        mListener = listener;
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
        holder.mName.setText(mValues.get(position).name);
        holder.mSegmentedBar.setValueWithUnit(mValues.get(position).value, mValues.get(position).unit);
        holder.mSegmentedBar.setSegments(mValues.get(position).segments);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onAQReadingsFragmentInteraction(holder.mItem);
                }
            }
        });
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

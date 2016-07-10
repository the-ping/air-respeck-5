package com.specknet.airrespeck.fragments.items;


import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.fragments.AirQualityFragment;
import com.specknet.airrespeck.utils.CircularGauge;

import java.util.ArrayList;
import java.util.List;


/**
 * {@link RecyclerView.Adapter} that can display a {@link ReadingItem} and makes a call
 * to the specified {@link AirQualityFragment.OnAirQualityFragmentInteractionListener}.
 */
public class ReadingItemRecyclerViewAdapter extends
        RecyclerView.Adapter<ReadingItemRecyclerViewAdapter.ViewHolder> {

    private final Context mContext;
    private final int mLayoutResId;
    private final List<ReadingItem> mValues;
    private final AirQualityFragment.OnAirQualityFragmentInteractionListener mListener;

    public ReadingItemRecyclerViewAdapter(Context context,
                                          int layoutResId,
                                          List<ReadingItem> items,
                                          AirQualityFragment.OnAirQualityFragmentInteractionListener listener) {
        mContext = context;
        mLayoutResId = layoutResId;
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(mLayoutResId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        if (holder.mGauge != null) {
            holder.mGauge.getModels().get(0).setProgress(mValues.get(position).value);
        }
        holder.mUnits.setText(mValues.get(position).units);
        holder.mValue.setText(String.valueOf(mValues.get(position).value));
        holder.mName.setText(mValues.get(position).name);

        // No listener needed for now
        /*holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onAirQualityFragmentInteraction(holder.mItem);
                }
            }
        });*/
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public CircularGauge mGauge;
        public final TextView mUnits;
        public final TextView mValue;
        public final TextView mName;
        public ReadingItem mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mUnits = (TextView) view.findViewById(R.id.units);
            mValue = (TextView) view.findViewById(R.id.value);
            mName = (TextView) view.findViewById(R.id.name);
            mGauge = (CircularGauge) view.findViewById(R.id.gauge);

            if (mGauge != null) {
                // Get colors
                int bgColor = ContextCompat.getColor(mContext, R.color.md_grey_500);
                int[] gaugeColors = {
                        ContextCompat.getColor(mContext, R.color.md_green_400),
                        ContextCompat.getColor(mContext, R.color.md_red_400)
                };

                // Set model(s)
                final ArrayList<CircularGauge.Model> models = new ArrayList<CircularGauge.Model>();
                models.add(new CircularGauge.Model("", 0, bgColor, gaugeColors));
                mGauge.setModels(models);
            }
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mName.getText() + ": " + mValue.getText() + " " + mUnits.getText() + "'";
        }
    }
}

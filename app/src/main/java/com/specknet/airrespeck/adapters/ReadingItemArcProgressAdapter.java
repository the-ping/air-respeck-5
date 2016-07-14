package com.specknet.airrespeck.adapters;


import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.fragments.AQReadingsFragment;
import com.specknet.airrespeck.lib.ArcProgress;
import com.specknet.airrespeck.models.ReadingItem;

import java.util.ArrayList;
import java.util.List;


/**
 * {@link RecyclerView.Adapter} that can display a {@link ReadingItem} and makes a call
 * to the specified {@link AQReadingsFragment.OnAQFragmentInteractionListener}.
 */
public class ReadingItemArcProgressAdapter extends
        RecyclerView.Adapter<ReadingItemArcProgressAdapter.ViewHolder> {

    private final Context mContext;
    private final List<ReadingItem> mValues;
    private final AQReadingsFragment.OnAQFragmentInteractionListener mListener;

    public ReadingItemArcProgressAdapter(Context context,
                                         List<ReadingItem> items,
                                         AQReadingsFragment.OnAQFragmentInteractionListener listener) {
        mContext = context;
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_arcprogress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mName.setText(holder.mItem.name);
        holder.mUnit.setText(holder.mItem.unit);
        holder.mValue.setText(String.valueOf(holder.mItem.value));

        final ArrayList<ArcProgress.Model> models = new ArrayList<ArcProgress.Model>();
        if (holder.mItem.colors == null) {
            models.add(new ArcProgress.Model(
                    "",
                    holder.mItem.minVal,
                    holder.mItem.maxVal,
                    holder.mItem.value,
                    holder.mItem.bgColor,
                    holder.mItem.defaultColor));
        }
        else {
            models.add(new ArcProgress.Model(
                    "",
                    holder.mItem.minVal,
                    holder.mItem.maxVal,
                    holder.mItem.value,
                    holder.mItem.bgColor,
                    holder.mItem.colors));
        }
        holder.mArcProgress.setModels(models);

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
        public final TextView mUnit;
        public final TextView mValue;
        public ArcProgress mArcProgress;
        public ReadingItem mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mName = (TextView) view.findViewById(R.id.name);
            mUnit = (TextView) view.findViewById(R.id.unit);
            mValue = (TextView) view.findViewById(R.id.value);
            mArcProgress = (ArcProgress) view.findViewById(R.id.arc_progress);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mName.getText() + ": " + mValue.getText() + " " + mUnit.getText() + "'";
        }
    }
}

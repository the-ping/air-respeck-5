package com.specknet.airrespeck.fragments.items;


import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.specknet.airrespeck.R;


public class ReadingItemArrayAdapter extends ArrayAdapter<ReadingItem> {

    // View lookup cache
    private static class ViewHolder {
        TextView name;
        TextView units;
        TextView value;
    }

    public ReadingItemArrayAdapter(Context context,
                                   ArrayList<ReadingItem> items) {
        super(context, R.layout.reading_list_view_item, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        ReadingItem item = getItem(position);

        // View lookup cache stored in tag
        ViewHolder viewHolder;

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.reading_list_view_item, parent, false);

            // Lookup view for data population
            viewHolder.name = (TextView) convertView.findViewById(R.id.name);
            viewHolder.units = (TextView) convertView.findViewById(R.id.units);
            viewHolder.value = (TextView) convertView.findViewById(R.id.value);

            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Populate the data into the template view using the data object
        viewHolder.name.setText(item.name);
        viewHolder.units.setText(item.units);
        viewHolder.value.setText(String.valueOf(item.value));

        // Return the completed view to render on screen
        return convertView;
    }
}

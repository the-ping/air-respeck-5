package com.specknet.airrespeck.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.specknet.airrespeck.R;

import java.util.ArrayList;
import java.util.Map;

public class HashMapAdapter extends BaseAdapter {
    private final ArrayList mData;

    public HashMapAdapter(Map<String, String> map) {
        mData = new ArrayList();
        mData.addAll(map.entrySet());
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Map.Entry<String, String> getItem(int position) {
        return (Map.Entry) mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO implement you own logic with ID
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View result;

        if (convertView == null) {
            result = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_config_view, parent, false);
        } else {
            result = convertView;
        }


        Map.Entry<String, String> item = getItem(position);

        // TODO replace findViewById by ViewHolder

        ((TextView) result.findViewById(R.id.key)).setText(item.getKey());
        ((TextView) result.findViewById(R.id.value)).setText(item.getValue());

        return result;
    }
}
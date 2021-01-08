package com.specknet.airrespeck.activities;

import android.content.CursorLoader;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ListView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.HashMapAdapter;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

/**
 * Page to view the configuration set by the pairing app
 */

public class ConfigViewActivity extends AppCompatActivity {

    private ListView configList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_config);

        configList = (ListView) findViewById(R.id.list_view_config_view);
        HashMapAdapter adapter = new HashMapAdapter(Utils.getInstance().getConfig(this));
        configList.setAdapter(adapter);
    }

    private void loadValuesFromContentProvider() {
        CursorLoader cursorLoader = new CursorLoader(getBaseContext(), Constants.Config.CONFIG_CONTENT_URI,
                null, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getBaseContext(),
                R.layout.list_item_config_view,
                cursor,
                new String[]{"Name", "Value"},
                new int[]{R.id.key, R.id.value}, 0);

        configList.setAdapter(adapter);
    }
}

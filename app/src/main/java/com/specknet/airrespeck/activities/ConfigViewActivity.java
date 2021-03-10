package com.specknet.airrespeck.activities;

import android.content.CursorLoader;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.TextView;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.adapters.HashMapAdapter;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.util.Map;

/**
 * Page to view the configuration set by the pairing app
 */

public class ConfigViewActivity extends AppCompatActivity {

    private ListView configList;
    private String subjectID;
    private String respeckID;
    private TextView show_subjectid;
    private TextView show_respeckid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_config);

//        configList = (ListView) findViewById(R.id.list_view_config_view);
//        HashMapAdapter adapter = new HashMapAdapter(Utils.getInstance().getConfig(this));
//        configList.setAdapter(adapter);

        // Load config variables
        Map<String, String> config = Utils.getInstance().getConfig(this);

        subjectID = config.get(Constants.Config.SUBJECT_ID);
        respeckID = config.get(Constants.Config.RESPECK_UUID);
        show_subjectid = findViewById(R.id.subjectid_text);
        show_respeckid = findViewById(R.id.respeckid_text);
        show_subjectid.setText("Subject ID: " + subjectID);
        show_respeckid.setText("Respeck ID: \n" + respeckID);

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

package com.specknet.airrespeck.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.specknet.airrespeck.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;

public class UserProfileActivity extends BaseActivity {

    private HashMap<String, String> mGenderMap;
    private HashMap<String, String> mUsertypeMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) { e.printStackTrace(); }

        setupHashMaps();
        String fullName = mCurrentUser.getFirstName() + " " + mCurrentUser.getLastName();
        DateFormat mDateFormatter = SimpleDateFormat.getDateInstance(DateFormat.LONG);

        TextView name = (TextView) findViewById(R.id.user_name);
        TextView birthDate = (TextView) findViewById(R.id.user_birth_date);
        TextView gender = (TextView) findViewById(R.id.user_gender);
        TextView type = (TextView) findViewById(R.id.user_type);
        TextView illiterate = (TextView) findViewById(R.id.user_illiterate);

        name.setText(fullName);
        birthDate.setText(mDateFormatter.format(mCurrentUser.getBirthDate()));
        gender.setText(mGenderMap.get(mCurrentUser.getGender()));
        type.setText(mUsertypeMap.get(String.valueOf(mCurrentUser.getUserType())));
        illiterate.setText(mCurrentUser.isIlliterate() ? R.string.user_profile_yes : R.string.user_profile_no);
    }

    /**
     * Initialize hashmaps used to load data from spinner input controls.
     * IMPORTANT: string-array resource must match the ones used in the corresponding layouts.
     */
    private void setupHashMaps() {
        // Gender
        String[] genderArrayValue = getResources().getStringArray(R.array.new_user_gender_list);
        String[] genderArrayKey = getResources().getStringArray(R.array.new_user_gender_list_values);

        mGenderMap = new HashMap<String, String>();

        for (int i = 0; i < genderArrayValue.length && i < genderArrayKey.length; i++) {
            mGenderMap.put(genderArrayKey[i], genderArrayValue[i]);
        }

        // User type
        String[] usertypeArrayValue = getResources().getStringArray(R.array.new_user_usertype_list);
        String[] usertypeArrayKey = getResources().getStringArray(R.array.new_user_usertype_list_values);

        mUsertypeMap = new HashMap<String, String>();

        for (int i = 0; i < usertypeArrayValue.length && i < usertypeArrayKey.length; i++) {
            mUsertypeMap.put(usertypeArrayKey[i], usertypeArrayValue[i]);
        }
    }
}

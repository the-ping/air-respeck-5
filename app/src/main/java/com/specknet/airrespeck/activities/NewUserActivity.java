package com.specknet.airrespeck.activities;


import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.internal.LinkedTreeMap;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.http.HttpApi;
import com.specknet.airrespeck.datamodels.User;
import com.specknet.airrespeck.utils.NetworkUtils;
import com.specknet.airrespeck.utils.PreferencesUtils;
import com.specknet.airrespeck.utils.Utils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class NewUserActivity extends BaseActivity implements View.OnClickListener {

    private EditText mFirstName;
    private EditText mLastName;
    private EditText mBirthDate;
    private Spinner mUsertype;
    private Spinner mGender;
    private CheckBox mIlliterate;
    private Button mRegisterBtn;
    private TextView mMessage;

    private DatePickerDialog mBirthDatePickerDialog;
    private DateFormat mDateFormatter;
    private long mTimeInMillis;

    private User mUser;

    private HashMap<String,String> mGenderMap;
    private HashMap<String,String> mUsertypeMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFirstName = (EditText) findViewById(R.id.new_user_first_name);
        mLastName = (EditText) findViewById(R.id.new_user_last_name);
        mBirthDate = (EditText) findViewById(R.id.new_user_birth_date);
        mUsertype = (Spinner) findViewById(R.id.new_user_usertype);
        mGender = (Spinner) findViewById(R.id.new_user_gender);
        mIlliterate = (CheckBox) findViewById(R.id.new_user_illiterate);
        mRegisterBtn = (Button) findViewById(R.id.new_user_register);
        mMessage = (TextView) findViewById(R.id.new_user_message);

        if (mRegisterBtn != null) {
            mRegisterBtn.setOnClickListener(this);
        }
        if (mMessage != null) {
            mMessage.setText("");
        }
        setupHashMaps();
        setupDatePicker();
    }

    @Override
    public void onClick(View v) {
        if(v.equals(mBirthDate)) {
            mBirthDatePickerDialog.show();
        }
        else if (v.equals(mRegisterBtn)) {
            if (mFirstName.getText().toString().isEmpty() ||
                    mLastName.getText().toString().isEmpty() ||
                    mBirthDate.getText().toString().isEmpty()) {
                Toast.makeText(getApplicationContext(), R.string.new_user_empty_fields, Toast.LENGTH_SHORT).show();
            }
            else {
                NetworkUtils networkUtils = NetworkUtils.getInstance(getApplicationContext());
                if (networkUtils.isNetworkAvailable()) {
                    createUserApiRequest();
                }
                else {
                    Toast.makeText(this, R.string.no_network, Toast.LENGTH_SHORT).show();
                }
            }
        }
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

        for (int i = 0; i < genderArrayValue.length && i < genderArrayKey.length; i++)
        {
            mGenderMap.put(genderArrayValue[i], genderArrayKey[i]);
        }

        // User type
        String[] usertypeArrayValue = getResources().getStringArray(R.array.new_user_usertype_list);
        String[] usertypeArrayKey = getResources().getStringArray(R.array.new_user_usertype_list_values);

        mUsertypeMap = new HashMap<String, String>();

        for (int i = 0; i < usertypeArrayValue.length && i < usertypeArrayKey.length; i++)
        {
            mUsertypeMap.put(usertypeArrayValue[i], usertypeArrayKey[i]);
        }
    }

    /**
     * Setup date picker.
     */
    private void setupDatePicker() {
        mDateFormatter = SimpleDateFormat.getDateInstance(DateFormat.LONG);

        mBirthDate.setInputType(InputType.TYPE_NULL);
        mBirthDate.requestFocus();
        mBirthDate.setOnClickListener(this);
        mBirthDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mBirthDatePickerDialog.show();
                }
                v.clearFocus();
            }
        });

        Calendar calendar = Calendar.getInstance();
        mBirthDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                mBirthDate.setText(mDateFormatter.format(newDate.getTime()));
                mTimeInMillis = newDate.getTimeInMillis();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Build a new user instance.
     * @return User The user instance.
     */
    private User buildUserInstance() {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(mTimeInMillis);

        return new User(
                "",
                mFirstName.getText().toString(),
                mLastName.getText().toString(),
                //String.valueOf(mTimeInMillis),
                date.getTime(),
                mGenderMap.get(mGender.getSelectedItem().toString()),
                Integer.parseInt(mUsertypeMap.get(mUsertype.getSelectedItem().toString())),
                mIlliterate.isChecked());
    }

    /**
     * Set POST request to server to create a new user.
     */
    private void createUserApiRequest() {
        HttpApi api = HttpApi.getInstance();

        // Prepare the HTTP request & asynchronously execute HTTP request
        mUser = buildUserInstance();
        Call<LinkedTreeMap> call = api.getService().createUser(mUser);

        call.enqueue(new Callback<LinkedTreeMap>() {
            /**
             * onResponse is called when any kind of response has been received.
             */
            @Override
            public void onResponse(Call<LinkedTreeMap> call, Response<LinkedTreeMap> response) {
                // http response status code + headers
                System.out.println("Response status code: " + response.code());

                // isSuccess is true if response code => 200 and <= 300
                if (!response.isSuccessful()) {
                    // print response body if unsuccessful
                    try {
                        System.out.println(response.errorBody().string());
                    } catch (IOException e) { }
                    return;
                }

                // if parsing the JSON body failed, `response.body()` returns null
                LinkedTreeMap decodedResponse = response.body();
                if (decodedResponse == null) {
                    return;
                }

                // at this point the JSON body has been successfully parsed
                System.out.println("Response: user = " + decodedResponse.toString());

                // Setup app
                setupApp(decodedResponse.get("unique_id").toString());
            }

            /**
             * onFailure gets called when the HTTP request didn't get through.
             * For instance if the URL is invalid / host not reachable
             */
            @Override
            public void onFailure(Call<LinkedTreeMap> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                mMessage.setText(R.string.network_required);
            }
        });
    }

    /**
     * Setup the app (i.e. UI), save the user data, and redirect to the home screen.
     */
    private void setupApp(final String userUniqueId) {
        // Set the returned Unique Id in the user model and save in db
        mUser.setUniqueId(userUniqueId);
        mUser.save();

        // Setup UI
        Utils utils = Utils.getInstance(getApplicationContext());
        utils.setupUI(mUser);

        // Go to Main Activity
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        this.finish();
    }
}

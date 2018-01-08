package com.specknet.airrespeck.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.models.KeyHolder;
import com.specknet.airrespeck.remote.SpecknetClient;
import com.specknet.airrespeck.remote.SpecknetService;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity which is run on the first time a phone is setup. It sends credentials to the server and receives
 * a security key back, which is then stored in shared preferences and used to encode all local storage.
 */

public class SecurityKeySetupActivity extends Activity {

    private Button mLoginButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_security_key);

        final EditText usernameEditText = (EditText) findViewById(R.id.user_text_field);
        final EditText passwordEditText = (EditText) findViewById(R.id.password_text_field);

        mLoginButton = (Button) findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoginButton.setEnabled(false);
                requestSecurityKey(usernameEditText.getText().toString(), passwordEditText.getText().toString());
            }
        });
    }

    private void requestSecurityKey(String username, String password) {
        Utils utils = Utils.getInstance();
        Map<String, String> config = utils.getConfig(this);
        final String projectID = config.get(Constants.Config.SUBJECT_ID).substring(0, 2);

        SpecknetService specknetService = SpecknetClient.getSpecknetService();

        specknetService.makeUploadKey(username, password, projectID,
                Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)).enqueue(
                new Callback<KeyHolder>() {
                    @Override
                    public void onResponse(Call<KeyHolder> call, Response<KeyHolder> response) {
                        if (response.isSuccessful()) {
                            saveKey(response.body().getKey(), projectID);
                        } else {
                            Toast.makeText(SecurityKeySetupActivity.this, "Username or password incorrect.",
                                    Toast.LENGTH_LONG).show();
                        }
                        mLoginButton.setEnabled(true);
                    }

                    @Override
                    public void onFailure(Call<KeyHolder> call, Throwable t) {
                        Log.e("DF", "Unable to submit post to API: " + Log.getStackTraceString(t));
                        Toast.makeText(SecurityKeySetupActivity.this,
                                "Could not communicate with server. Please check network connection!",
                                Toast.LENGTH_LONG).show();
                        mLoginButton.setEnabled(true);
                    }
                });
    }

    public void saveKey(String key, String projectID) {
        SharedPreferences sharedPref = getSharedPreferences(Constants.SECURITY_KEY_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.SECURITY_KEY, key);
        // Also save project ID
        editor.putString(Constants.PROJECT_ID, projectID);
        editor.apply();

        Toast.makeText(SecurityKeySetupActivity.this, "Key successfully created!",
                Toast.LENGTH_LONG).show();

        // Start main activity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}

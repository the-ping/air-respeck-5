package com.specknet.airrespeck.activities;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.datamodels.User;
import com.specknet.airrespeck.http.HttpApi;
import com.specknet.airrespeck.utils.PreferencesUtils;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class InitialSetupActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial_setup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (User.isTableEmpty()) {
            // No user found, go to New user Activity
            goToNewUserScreen();
        }
        else {
            // User found, get user data from the server
            PreferencesUtils.getInstance(getApplicationContext());

            // Get user profile only on initial app startup
            if (PreferencesUtils.getInstance().getBoolean(PreferencesUtils.Key.IS_APP_INITIAL_STARTUP)) {
                User user = User.getUser();
                getUserDataApiRequest(user.getUniqueId());

                // Update flag
                PreferencesUtils.getInstance().put(PreferencesUtils.Key.IS_APP_INITIAL_STARTUP, false);
            }
            else {
                goToMainScreen();
            }
        }
    }

    /**
     * Send a GET request to the server to get the user data.
     */
    private void getUserDataApiRequest(final String userUniqueId) {
        // This can be used in any Activity, etc.
        HttpApi api = HttpApi.getInstance();

        // Prepare the HTTP request & asynchronously execute HTTP request
        Call<User> call = api.getService().getUser(userUniqueId);

        call.enqueue(new Callback<User>() {
            /**
             * onResponse is called when any kind of response has been received.
             */
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
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
                User user = response.body();
                if (user == null) {
                    return;
                }

                // at this point the JSON body has been successfully parsed
                processResponse(user);
            }

            /**
             * onFailure gets called when the HTTP request didn't get through.
             * For instance if the URL is invalid / host not reachable
             */
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                //Toast.makeText(getApplicationContext(), t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                // Error during the request, go to Main Activity without updating the user profile
                goToMainScreen();
            }
        });
    }

    /**
     *
     * @param user User The user data returned by the server.
     */
    private void processResponse(User user) {
        if (user.isActive()) {
            // No deletion request, go to Main Activity
            goToMainScreen();
        }
        else {
            // TODO handle user deletion
        }
    }

    /**
     * Go to new user activity and finish the current activity.
     */
    private void goToNewUserScreen() {
        startActivity(new Intent(this, NewUserActivity.class));
        this.finish();
    }

    /**
     * Go to main activity and finish the current activity.
     */
    private void goToMainScreen() {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        this.finish();
    }

}

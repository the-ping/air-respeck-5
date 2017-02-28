package com.specknet.airrespeck.activities;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.lazydroid.autoupdateapk.AutoUpdateApk;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.datamodels.User;
import com.specknet.airrespeck.http.HttpApi;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class InitialSetupActivity extends BaseActivity {

    private AutoUpdateApk aua;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial_setup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        aua = new AutoUpdateApk(getApplicationContext());
        aua.enableMobileUpdates();
        //aua.checkUpdatesManually();

        // Create directory on external storage if it doesn't exist
        // Create prediction summary directory if it doesn't exist
        File directory = new File(Constants.EXTERNAL_DIRECTORY_STORAGE_PATH);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            Log.i("DF", "Directory created: " + directory);
            if (!created) {
                throw new RuntimeException("Couldn't create folder on external storage");
            }
        }

        Utils mUtils = Utils.getInstance(this);
        // Look for storage related configs
        boolean isAirspeckEnabled = Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_AIRSPECK_ENABLED));
        boolean isStoreDataLocally = Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_STORE_DATA_LOCALLY));
        boolean isStoreMergedFile = (Boolean.parseBoolean(
                mUtils.getProperties().getProperty(Constants.Config.IS_STORE_MERGED_FILE)) && isAirspeckEnabled);

        // If data storage files don't exist and we want to store data, create them with the correct header!
        if (isStoreDataLocally) {
            File respeckFile = new File(Constants.RESPECK_DATA_FILE_PATH);
            if (!respeckFile.exists()) {
                Log.i("DF", "created respeck file with header " + respeckFile.getPath());
                mUtils.writeToExternalStorageFile(Constants.RESPECK_DATA_HEADER + "\n",
                        Constants.RESPECK_DATA_FILE_PATH);
            }
            if (isAirspeckEnabled) {
                File airspeckFile = new File(Constants.AIRSPECK_DATA_FILE_PATH);
                if (!airspeckFile.exists()) {
                    mUtils.writeToExternalStorageFile(Constants.AIRSPECK_DATA_HEADER + "\n",
                            Constants.AIRSPECK_DATA_FILE_PATH);
                }
            }
            if (isStoreMergedFile) {
                File mergedFile = new File(Constants.MERGED_DATA_FILE_PATH);
                if (!mergedFile.exists()) {
                    mUtils.writeToExternalStorageFile(Constants.MERGED_DATA_HEADER + "\n",
                            Constants.MERGED_DATA_FILE_PATH);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        setupUser();

        // NOTE: This will not be used on initial deployment of the application. Instead, a user
        // will be created using data from the properties configuration file stored in the external
        // storage directory. The above temporal method will handle this.

        /*if (User.isTableEmpty()) {
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
        }*/
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
     * @param user User The user data returned by the server.
     */
    private void processResponse(User user) {
        if (user.isActive()) {
            // No deletion request, go to Main Activity
            goToMainScreen();
        } else {
            // TODO handle user deletion
            // First, check with Tape lib that everything has been uploaded to the server.
            // Then, delete user from local database
            //User.deleteUserByUniqueId(user.getUniqueId());
            // Finally, restart this activity to prompt user creation
            //this.recreate();
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


    /**
     * Method to handle user creation on startup using data from the properties configuration
     * file stored in the external storage directory.
     * <p>
     * Data needed to create a user:
     * - user_id
     * - user_age
     * - user_type [Values: Subject (1), Researcher (2)]
     */
    private void setupUser() {
        Utils utils = Utils.getInstance(getApplicationContext());

        try {
            // Get properties
            String id = utils.getProperties().getProperty(Constants.Config.PATIENT_ID);
            int age = Integer.parseInt(utils.getProperties().getProperty(Constants.Config.PATIENT_AGE));
            int type = Integer.parseInt(utils.getProperties().getProperty(Constants.Config.USER_TYPE));

            // Parse data
            String firstName = (type == 1) ? "Subject" : "Researcher";
            Calendar now = Calendar.getInstance();
            now.add(Calendar.YEAR, -age);

            // No user found
            if (User.isTableEmpty()) {
                // Create user
                User newUser = new User(id, firstName, id, now.getTime(), "M", type, false);
                newUser.save();

                // Setup UI
                utils.setupUI(newUser);
            }
            // User found
            else {
                // Check for changes in the id
                User currentUser = User.getUser();
                if (!currentUser.getUniqueId().equalsIgnoreCase(id)) {
                    // The id has been changed, delete current user and create a new user
                    User.deleteUserByUniqueId(currentUser.getUniqueId());

                    // Create user
                    User newUser = new User(id, firstName, id, now.getTime(), "M", type, false);
                    newUser.save();

                    // Setup UI
                    utils.setupUI(newUser);
                }
            }

            // Go to main activity
            goToMainScreen();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error reading properties file.", Toast.LENGTH_LONG).show();
        }
    }
}

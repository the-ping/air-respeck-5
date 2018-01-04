package com.specknet.airrespeck.activities;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Activity which is run on the first time a phone is setup. It sends credentials to the server and receives
 * a security key back, which is then stored in shared preferences and used to encode all local storage.
 */

public class SecurityKeySetupActivity extends Activity {
    private final String REQUEST_KEY_URL = "https://dashboard.specknet.uk/make_upload_key";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_security_key);

        final EditText usernameEditText = (EditText) findViewById(R.id.user_text_field);
        final EditText passwordEditText = (EditText) findViewById(R.id.password_text_field);

        Button loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSecurityKey(usernameEditText.getText().toString(), passwordEditText.getText().toString());
            }
        });
    }

    private void requestSecurityKey(String username, String password) {
        String data = null;
        Utils utils = Utils.getInstance();
        Map<String, String> config = utils.getConfig(this);
        try {
            data = URLEncoder.encode("username", "UTF-8")
                    + "=" + URLEncoder.encode(username, "UTF-8");
            data += "&" + URLEncoder.encode("password", "UTF-8") + "="
                    + URLEncoder.encode(password, "UTF-8");
            data += "&" + URLEncoder.encode("project_id", "UTF-8") + "="
                    + URLEncoder.encode(config.get(Constants.Config.SUBJECT_ID).substring(0, 2), "UTF-8");
            data += "&" + URLEncoder.encode("android_id", "UTF-8") + "="
                    + URLEncoder.encode(
                    Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        new CallAPI().execute(REQUEST_KEY_URL, data);
    }

    public static class CallAPI extends AsyncTask<String, String, String> {
        public CallAPI() {
            //set context variables if required
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            Log.i("DF", "do in background request");
            String urlString = params[0]; // URL to call
            String data = params[1]; //data to post
            OutputStream out;
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                //urlConnection.setRequestProperty("Key", "Value");
                urlConnection.setDoOutput(true);

                out = new BufferedOutputStream(urlConnection.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));

                writer.write(data);
                writer.flush();
                writer.close();
                out.close();

                urlConnection.connect();

                /*
                // Read Server Response
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    // Append server response in string
                    sb.append(line).append("\n");
                }

                return sb.toString();*/
                return "hello";
            } catch (Exception e) {
                Log.e("DF", "Error: " + Log.getStackTraceString(e) + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Log.i("DF", "ResultPOST: " + s);
            super.onPostExecute(s);
        }
    }
}

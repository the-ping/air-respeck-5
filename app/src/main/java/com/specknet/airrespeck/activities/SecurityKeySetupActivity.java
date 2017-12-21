package com.specknet.airrespeck.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.specknet.airrespeck.R;

/**
 * Activity which is run on the first time a phone is setup. It sends credentials to the server and receives
 * a security key back, which is then stored in shared preferences and used to encode all local storage.
 */

public class SecurityKeySetupActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_security_key);

        Button loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }
}

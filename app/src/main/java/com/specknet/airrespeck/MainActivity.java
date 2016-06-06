package com.specknet.airrespeck;

import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;

public class MainActivity extends AppCompatActivity {

    int buttonsNumber = 3;
    int screenWidth, screenHeight, buttonsWidth;
    int scaleWidth = 100, scaleHeight = 100;
    float buttonsPadding;
    LinearLayout menuContainer;
    LinearLayout.LayoutParams menuItemLayoutParameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get menu container layout and set layout parameters
        menuContainer = (LinearLayout)findViewById(R.id.lytMenu);
        menuItemLayoutParameters = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

        // Get screen size
        Display display = getWindowManager().getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);
        screenWidth = screenSize.x;
        screenHeight = screenSize.y;
        buttonsPadding = screenWidth * 5/100;
        buttonsWidth = (screenWidth - ((buttonsNumber*(int)buttonsPadding) + (int)buttonsPadding)) / buttonsNumber;

        // Create buttons
        createButton(getString(R.string.home), R.drawable.ic_home);
        createButton(getString(R.string.dashboard), R.drawable.ic_dashboard);
        createButton(getString(R.string.settings), R.drawable.ic_settings);

        // Create Update thread
        Thread myThread = null;
        Runnable runnable = new updateLoop();
        myThread= new Thread(runnable);
        myThread.start();
    }

    private void createButton(final String label, final int image) {
        Button button = new Button(this);
        button.setText(label);
        button.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        button.setCompoundDrawablesWithIntrinsicBounds(0, image, 0, 0);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(this, SettingsActivity.class));
            }
        });

        menuContainer.addView(button, menuItemLayoutParameters);
    }

    private void setRespiratoryRate(final String value) {
        TextView rr = (TextView)findViewById(R.id.tvRespiratoryRateVal);
        rr.setText(value);
    }

    private void setPM10(final String value) {
        TextView pm10 = (TextView)findViewById(R.id.tvPM10Val);
        pm10.setText(value);
    }

    private void setPM2_5(final String value) {
        TextView pm2_5 = (TextView)findViewById(R.id.tvPM2_5Val);
        pm2_5.setText(value);
    }

    public void updateValues() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    Date dt = new Date();
                    long t = dt.getTime();
                    int seconds = (int) ((t / 1000) % 60);

                    setRespiratoryRate(String.valueOf(seconds));
                    setPM10(String.valueOf(seconds));
                    setPM2_5(String.valueOf(seconds));
                } catch (Exception e) {}
            }
        });
    }

    class updateLoop implements Runnable {
        // @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    updateValues();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch(Exception e){}
            }
        }
    }
}

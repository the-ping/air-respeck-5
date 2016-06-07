package com.specknet.airrespeck;


import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.Date;


public class MainActivity extends AppCompatActivity {

    private MainDataFragment mMainDataFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainDataFragment = (MainDataFragment)
                getSupportFragmentManager().findFragmentById(R.id.main_data_fragment);

        Thread updateThread = null;
        Runnable runnable = new updateLoop();
        updateThread= new Thread(runnable);
        updateThread.start();

        /*FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        //ft.replace(R.id.content, new MainDataFragment());
        ft.add(R.id.content_layout, new MainDataFragment());
        ft.add(R.id.content_layout, new FeedbackFragment());
        ft.commit();*/
    }

    public void updateValues() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    Date dt = new Date();
                    long t = dt.getTime();
                    int seconds = (int) ((t / 1000) % 60);

                    mMainDataFragment.setRespiratoryRate(String.valueOf(seconds));
                    mMainDataFragment.setPM10(String.valueOf(seconds));
                    mMainDataFragment.setPM2_5(String.valueOf(seconds));
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

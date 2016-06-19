package com.specknet.airrespeck;


import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Date;


public class MainActivity extends AppCompatActivity implements MenuFragment.OnMenuSelectedListener {

    protected final String TAG_HOME = "HOME";
    protected final String TAG_DASHBOARD = "DASHBOARD";
    Toolbar toolbar;
    private HomeFragment mHomeFragment;
    private DashboardFragment mDashboardFragment;
    private Fragment mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            mHomeFragment = new HomeFragment();
            mDashboardFragment = new DashboardFragment();

            FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
            trans.add(R.id.content, mHomeFragment, TAG_HOME);
            trans.commit();

            mCurrentFragment = mHomeFragment;

            Thread updateThread = null;
            Runnable runnable = new updateLoop();
            updateThread = new Thread(runnable);
            updateThread.start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu, this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //menu.getItem(0).setVisible(false); // here pass the index of save menu item
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStattement
        if (id == R.id.action_settings) {
            return true;
        }
        else if (id == R.id.action_air_speck) {
            startActivity(new Intent(this, AirSpeckActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onButtonSelected(int buttonId) {
        switch (buttonId) {
            // Home
            case 0:
                replaceFragment(mHomeFragment, TAG_HOME);
                break;
            // Dashboard
            case 1:
                replaceFragment(mDashboardFragment, TAG_DASHBOARD);
                //toolbar.getMenu().setGroupVisible(R.id.main_menu_group, true);
                break;
            // Settings
            case 2:
                //toolbar.getMenu().setGroupVisible(R.id.main_menu_group, false);
                //toolbar.getMenu().clear();
                break;
        }
    }

    public void replaceFragment(Fragment fragment, String tag) {
        replaceFragment(fragment, tag, false);
    }

    public void replaceFragment(Fragment fragment, String tag, boolean addToBackStack) {
        if (fragment.isVisible()) {
            return;
        }

        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        //trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        trans.replace(R.id.content, fragment, tag);
        if (addToBackStack) {
            trans.addToBackStack(null);
        }
        trans.commit();

        mCurrentFragment = fragment;
    }



    public void updateValues() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    Date dt = new Date();
                    long t = dt.getTime();
                    int seconds = (int) ((t / 1000) % 60);

                    mHomeFragment.setRespiratoryRate(seconds);
                    mHomeFragment.setPM10(seconds);
                    mHomeFragment.setPM2_5(seconds);
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

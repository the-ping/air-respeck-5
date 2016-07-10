package com.specknet.airrespeck;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.specknet.airrespeck.fragments.DashboardFragment;
import com.specknet.airrespeck.fragments.HomeFragment;
import com.specknet.airrespeck.fragments.AirQualityFragment;
import com.specknet.airrespeck.fragments.MenuFragment;
import com.specknet.airrespeck.fragments.items.ReadingItem;
import com.specknet.airrespeck.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends BaseActivity implements
        MenuFragment.OnMenuSelectedListener {

    private Thread mUpdateThread;

    private static final String TAG_HOME_FRAGMENT = "HOME_FRAGMENT";
    private static final String TAG_AIR_QUALITY_FRAGMENT = "AIR_QUALITY_FRAGMENT";
    private static final String TAG_DASHBOARD_FRAGMENT = "DASHBOARD_FRAGMENT";

    private HomeFragment mHomeFragment;
    private AirQualityFragment mAirQualityFragment;
    private DashboardFragment mDashboardFragment;
    private Fragment mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize fragments
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState != null) {
            mHomeFragment =
                    (HomeFragment) fm.getFragment(savedInstanceState, TAG_HOME_FRAGMENT);
            mAirQualityFragment =
                    (AirQualityFragment) fm.getFragment(savedInstanceState, TAG_AIR_QUALITY_FRAGMENT);
            mDashboardFragment =
                    (DashboardFragment) fm.getFragment(savedInstanceState, TAG_DASHBOARD_FRAGMENT);
        }
        else {
            mHomeFragment = (HomeFragment) fm.findFragmentByTag(TAG_HOME_FRAGMENT);
            mAirQualityFragment = (AirQualityFragment) fm.findFragmentByTag(TAG_AIR_QUALITY_FRAGMENT);
            mDashboardFragment = (DashboardFragment) fm.findFragmentByTag(TAG_DASHBOARD_FRAGMENT);
        }

        if (mHomeFragment == null) {
            mHomeFragment = new HomeFragment();
        }
        if (mAirQualityFragment == null) {
            mAirQualityFragment = new AirQualityFragment();
        }
        if (mDashboardFragment == null) {
            mDashboardFragment = new DashboardFragment();
        }

        // Choose layout
        if (mTabModePref) {
            setContentView(R.layout.activity_main_tabs);

            // Create the adapter that will return a fragment for each of the three
            // primary sections of the activity.
            SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
            sectionsPagerAdapter.setContext(getApplicationContext());

            // Set up the ViewPager with the sections adapter.
            ViewPager viewPager = (ViewPager) findViewById(R.id.container);
            if (viewPager != null) {
                viewPager.setAdapter(sectionsPagerAdapter);
            }

            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            if (tabLayout != null) {
                tabLayout.setupWithViewPager(viewPager);
            }
        }
        else {
            setContentView(R.layout.activity_main_buttons);

            if (mCurrentFragment == null) {
                fm.
                        beginTransaction().
                        replace(R.id.content, mHomeFragment, TAG_HOME_FRAGMENT).
                        commit();
                mCurrentFragment = mHomeFragment;
            }
        }

        // Add the toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);


        Runnable runnable = new updateLoop();
        mUpdateThread = new Thread(runnable);
        mUpdateThread.start();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        FragmentManager fm = getSupportFragmentManager();

        if (mHomeFragment != null) {
            try {
                fm.putFragment(outState, TAG_HOME_FRAGMENT, mHomeFragment);
            } catch (IllegalStateException e) {}
        }

        if (mAirQualityFragment != null) {
            try {
                fm.putFragment(outState, TAG_AIR_QUALITY_FRAGMENT, mAirQualityFragment);
            } catch (IllegalStateException e) {}
        }

        if (mDashboardFragment != null) {
            try {
                fm.putFragment(outState, TAG_DASHBOARD_FRAGMENT, mDashboardFragment);
            } catch (IllegalStateException e) {}
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mUpdateThread.interrupt();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu, this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setVisible(mRespeckAppAccessPref);
        menu.getItem(1).setVisible(mAirspeckAppAccessPref);

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
            startActivity(new Intent(this, SettingsActivity.class));
        }
        else if (id == R.id.action_airspeck) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.airquality.sepa",
                        "com.airquality.sepa.DataCollectionActivity"));
                startActivity(intent);
            }
            catch (Exception e) {
                Toast.makeText(this, R.string.airspeck_not_found, Toast.LENGTH_LONG).show();
            }
        }
        else if (id == R.id.action_respeck) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.pulrehab",
                        "com.pulrehab.fragments.MainActivity"));
                startActivity(intent);
            }
            catch (Exception e) {
                Toast.makeText(this, R.string.respeck_not_found, Toast.LENGTH_LONG).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onButtonSelected(int buttonId) {
        switch (buttonId) {
            // Home
            case 0:
                replaceFragment(mHomeFragment, TAG_HOME_FRAGMENT);
                break;
            // Air Quality
            case 1:
                replaceFragment(mAirQualityFragment, TAG_AIR_QUALITY_FRAGMENT);
                break;
            // Dashboard
            case 2:
                replaceFragment(mDashboardFragment, TAG_DASHBOARD_FRAGMENT);
                break;
        }
    }

    /**
     * Replace the current fragment with the given one.
     * @param fragment Fragment New fragment.
     * @param tag String Tag for the new fragment.
     */
    public void replaceFragment(Fragment fragment, String tag) {
        replaceFragment(fragment, tag, false);
    }

    /**
     * Replace the current fragment with the given one.
     * @param fragment Fragment New fragment.
     * @param tag String Tag for the new fragment.
     * @param addToBackStack boolean Whether to add the previous fragment to the Back Stack, or not.
     */
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

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private Context mContext;
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setContext(Context context) {
            mContext = context;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return the relevant fragment per each page.
            switch (position) {
                case 0:
                    return mHomeFragment;
                case 1:
                    return mAirQualityFragment;
                case 2:
                    return mDashboardFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Drawable image;
            SpannableString sb;
            ImageSpan imageSpan;

            switch (position) {
                case 0:
                    if (mIconsInTabsPref) {
                        image = ContextCompat.getDrawable(mContext, R.drawable.vec_home);
                        image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
                        sb = new SpannableString("  " + getString(R.string.menu_home));
                        imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
                        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        return sb;
                    }
                    return getString(R.string.menu_home);
                case 1:
                    if (mIconsInTabsPref) {
                        image = ContextCompat.getDrawable(mContext, R.drawable.vec_air);
                        image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
                        sb = new SpannableString("  " + getString(R.string.menu_air_quality));
                        imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
                        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        return sb;
                    }
                    return getString(R.string.menu_air_quality);
                case 2:
                    if (mIconsInTabsPref) {
                        image = ContextCompat.getDrawable(mContext, R.drawable.vec_dashboard);
                        image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
                        sb = new SpannableString("  " + getString(R.string.menu_dashboard));
                        imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
                        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        return sb;
                    }
                    return getString(R.string.menu_dashboard);
            }
            return null;
        }
    }

    /**********************************************************************************************/

    public void updateValues() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    Date dt = new Date();
                    long t = dt.getTime();
                    int seconds = (int) ((t / 1000) % 60);

                    List<Integer> values = new ArrayList<Integer>();
                    values.add(seconds);
                    values.add(seconds);
                    values.add(seconds);

                    mHomeFragment.setReadings(values, seconds);

                    List<Integer> valuesAir = new ArrayList<Integer>();
                    valuesAir.add(seconds);
                    valuesAir.add(seconds);
                    valuesAir.add(seconds);
                    valuesAir.add(seconds);
                    valuesAir.add(seconds);
                    valuesAir.add(seconds);
                    valuesAir.add(seconds);
                    valuesAir.add(seconds);

                    mAirQualityFragment.setReadings(valuesAir);

                } catch (Exception e) {}
            }
        });
    }

    class updateLoop implements Runnable {
        // @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
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

package com.specknet.airrespeck;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.specknet.airrespeck.airspeck.AirSpeckActivity;
import com.specknet.airrespeck.fragments.DashboardFragment;
import com.specknet.airrespeck.fragments.HomeFragment;
import com.specknet.airrespeck.fragments.MenuFragment;
import com.specknet.airrespeck.respeck.RESpeckActivity;
import com.specknet.airrespeck.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity implements MenuFragment.OnMenuSelectedListener {

    Thread mUpdateThread;

    private static final String TAG_HOME_FRAGMENT = "HOME_FRAGMENT";
    private static final String TAG_DASHBOARD_FRAGMENT = "DASHBOARD_FRAGMENT";

    private HomeFragment mHomeFragment;
    private DashboardFragment mDashboardFragment;
    private Fragment mCurrentFragment;

    private SharedPreferences mSettings;
    private boolean mTabMode;

    private Toolbar mToolbar;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get settings
        mSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mTabMode = mSettings.getBoolean("main_menu_layout", false);

        // Initialize fragments
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState != null) {
            mHomeFragment =
                    (HomeFragment) fm.getFragment(savedInstanceState, TAG_HOME_FRAGMENT);
            mDashboardFragment =
                    (DashboardFragment) fm.getFragment(savedInstanceState, TAG_DASHBOARD_FRAGMENT);
        }
        else {
            mHomeFragment = (HomeFragment) fm.findFragmentByTag(TAG_HOME_FRAGMENT);
            mDashboardFragment = (DashboardFragment) fm.findFragmentByTag(TAG_DASHBOARD_FRAGMENT);
        }

        if (mHomeFragment == null) {
            mHomeFragment = new HomeFragment();
        }
        if (mDashboardFragment == null) {
            mDashboardFragment = new DashboardFragment();
        }

        // Choose layout
        if (mTabMode) {
            setContentView(R.layout.activity_main_tabs);

            // Create the adapter that will return a fragment for each of the three
            // primary sections of the activity.
            mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
            mSectionsPagerAdapter.setContext(getApplicationContext());

            // Set up the ViewPager with the sections adapter.
            mViewPager = (ViewPager) findViewById(R.id.container);
            mViewPager.setAdapter(mSectionsPagerAdapter);

            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(mViewPager);

            tabLayout.getTabAt(0).setIcon(Utils.menuIconsResId[0]);
            tabLayout.getTabAt(1).setIcon(Utils.menuIconsResId[3]);
        }
        else {
            setContentView(R.layout.activity_main_buttons);

            if (!mHomeFragment.isAdded()) {
                fm.
                        beginTransaction().
                        add(R.id.content, mHomeFragment, TAG_HOME_FRAGMENT).
                        commit();
            }

            mCurrentFragment = mHomeFragment;
        }

        // Add the toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);


        Runnable runnable = new updateLoop();
        mUpdateThread = new Thread(runnable);
        mUpdateThread.start();
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean newVal = mSettings.getBoolean("main_menu_layout", false);

        if (mTabMode != newVal) {
            mTabMode = newVal;

            // Refresh this activity.
            finish();
            startActivity(getIntent());
        }

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

        if (mDashboardFragment != null) {
            try {
                fm.putFragment(outState, TAG_DASHBOARD_FRAGMENT, mDashboardFragment);
            } catch (IllegalStateException e) {}
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("******************** Main Activity - DESTROY");
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
        menu.getItem(0).setVisible(mSettings.getBoolean("respeck_app_access", false));
        menu.getItem(1).setVisible(mSettings.getBoolean("airspeck_app_access", false));

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
            startActivity(new Intent(this, AirSpeckActivity.class));
        }
        else if (id == R.id.action_respeck) {
            startActivity(new Intent(this, RESpeckActivity.class));
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
            // Dashboard
            case 1:
                replaceFragment(mDashboardFragment, TAG_DASHBOARD_FRAGMENT);
                break;
            // Settings
            case 2:
                startActivity(new Intent(this, SettingsActivity.class));
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
                    /*return null;
                case 2:
                    return null;
                case 3:*/
                    return mDashboardFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 4 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.menu_home);
                    /*Drawable image = ContextCompat.getDrawable(mContext, Utils.menuIconsResId[position]);
                    image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
                    SpannableString sb = new SpannableString("   " + getString(R.string.menu_home));
                    ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
                    sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    return sb;*/
                case 1:
                    /*return getString(R.string.menu_health);
                case 2:
                    return getString(R.string.menu_air);
                case 3:*/
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

                    List<Integer> values = new ArrayList<>();
                    values.add(seconds);
                    values.add(seconds);
                    values.add(seconds);

                    mHomeFragment.setReadings(values, seconds);

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

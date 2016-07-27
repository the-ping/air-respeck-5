package com.specknet.airrespeck.adapters;


import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.specknet.airrespeck.R;

import java.util.ArrayList;
import java.util.List;


/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private Context mContext;
    private List<Fragment> mFragments;

    public SectionsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);

        mContext = context;
        mFragments = new ArrayList<Fragment>();
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return the relevant fragment per each page.
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return mContext.getString(R.string.menu_home);
            case 1:
                return mContext.getString(R.string.menu_air_quality);
            case 2:
                return mContext.getString(R.string.menu_graphs);
        }
        return null;
    }

    public void addFragment(Fragment fragment) {
        mFragments.add(fragment);
    }
}

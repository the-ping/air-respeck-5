package com.specknet.airrespeck.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class ping_SectionPagerAdapter extends FragmentPagerAdapter {

    private List<Fragment> fragmentlist = new ArrayList<>();
    private List<String> titlelist = new ArrayList<>();

    public ping_SectionPagerAdapter (@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @Override
    public Fragment getItem(int position) {
        return fragmentlist.get(position);
    }

    @Override
    public int getCount() {
        return fragmentlist.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return titlelist.get(position);
    }

    public void addFragment(Fragment fragment, String title) {
        fragmentlist.add(fragment);
        titlelist.add(title);
    }
}

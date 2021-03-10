package com.specknet.airrespeck.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.specknet.airrespeck.R;
import com.specknet.airrespeck.fragments.ActSumTabFragment;
import com.specknet.airrespeck.fragments.DiaryTabFragment;
import com.specknet.airrespeck.fragments.HomeTabFragment;
import com.specknet.airrespeck.fragments.LiveActTabFragment;
import com.specknet.airrespeck.fragments.RehabTabFragment;

import android.os.Bundle;
import android.view.MenuItem;

import java.util.HashSet;
import java.util.Set;

public class SubjectActivity extends AppCompatActivity {

    private Set<RESpeckDataObserver> respeckDataObservers = new HashSet<>();
    private Set<ConnectionStateObserver> connectionStateObservers = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject);

        // Load Tab Menu
        BottomNavigationView bottomNav = findViewById(R.id.bot_nav_menu);
        bottomNav.setOnNavigationItemReselectedListener(navListener);

        // set home tab as the main fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeTabFragment()).commit();

    }

    private BottomNavigationView.OnNavigationItemReselectedListener navListener =
            new BottomNavigationView.OnNavigationItemReselectedListener() {

                @Override
                public void onNavigationItemReselected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;

                    switch (item.getItemId()) {
                        case R.id.act_sum:
                            selectedFragment = new ActSumTabFragment();
                            break;
                        case R.id.live_act:
                            selectedFragment = new LiveActTabFragment();
                            break;
                        case R.id.home_subject:
                            selectedFragment = new HomeTabFragment();
                            break;
                        case R.id.diary:
                            selectedFragment = new DiaryTabFragment();
                            break;
                        case R.id.rehab:
                            selectedFragment = new RehabTabFragment();
                            break;

                    }
                    //replace current fragment? with selectedfragment
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            selectedFragment).commit();


                }
            };

    public void registerRESpeckDataObserver(RESpeckDataObserver observer) {
        respeckDataObservers.add(observer);
    }

    public void unregisterRESpeckDataObserver(RESpeckDataObserver observer) {
        respeckDataObservers.remove(observer);
    }

    public void registerConnectionStateObserver(ConnectionStateObserver observer) {
        connectionStateObservers.add(observer);
    }

    public void unregisterConnectionStateObserver(ConnectionStateObserver observer) {
        connectionStateObservers.remove(observer);
    }
}
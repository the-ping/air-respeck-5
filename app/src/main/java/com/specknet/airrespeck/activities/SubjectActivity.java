package com.specknet.airrespeck.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.specknet.airrespeck.R;

import android.content.Intent;
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

    }


}
package com.specknet.airrespeck.activities;


import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.datamodels.User;
import com.specknet.airrespeck.utils.PreferencesUtils;
import com.specknet.airrespeck.utils.ThemeUtils;


/**
 * A {@link PreferenceActivity} that presents a set of application settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
            new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For mData preferences, look up the correct display value in
                // the preference's 'entries' mData.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            }
            else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference name) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int fontSizePref = Integer.valueOf(PreferencesUtils.getInstance(getApplicationContext())
                .getString(PreferencesUtils.Key.FONT_SIZE, "1"));
        ThemeUtils themeUtils = ThemeUtils.getInstance();
        themeUtils.setTheme(fontSizePref);
        themeUtils.onActivityCreateSetTheme(this);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || SettingsFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows the settings preferences.
     */
    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            User user = User.getUserByUniqueId(PreferencesUtils.getInstance()
                    .getString(PreferencesUtils.Key.USER_ID));

            switch (user.getUserType()) {
                case 1:
                    addPreferencesFromResource(R.xml.pref_settings_basic_profile);
                    break;
                case 2:
                    addPreferencesFromResource(R.xml.pref_settings_advanced_profile);
                    // Available only for advanced users
                    bindPreferenceSummaryToValue(findPreference("menu_mode"));
                    break;
            }

            // Available for all
            bindPreferenceSummaryToValue(findPreference("font_size"));
            bindPreferenceSummaryToValue(findPreference("readings_mode_home_screen"));
            bindPreferenceSummaryToValue(findPreference("readings_mode_aqreadings_screen"));
        }

        @Override
        public void onResume() {
            super.onResume();
            // Set up a listener whenever a key changes
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            // Unregister the listener whenever a key changes
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("font_size")) {
                ListPreference listPref = (ListPreference) findPreference(key);

                ThemeUtils themeUtils = ThemeUtils.getInstance();
                themeUtils.setTheme(Integer.parseInt(listPref.getValue()));
                themeUtils.onActivityCreateSetTheme(this.getActivity());

                final FragmentManager fm = this.getActivity().getFragmentManager();
                fm.
                        beginTransaction().
                        detach(this).
                        attach(this).
                        commit();
            }
            else if (key.equals("menu_mode")) {
                ListPreference listPref = (ListPreference) findPreference(key);
                SwitchPreference switchPref = (SwitchPreference) findPreference("menu_tab_icons");

                if (listPref.getValue().equals("1")) {
                    switchPref.setEnabled(true);
                }
                else {
                    switchPref.setEnabled(false);
                }
            }
        }
    }
}

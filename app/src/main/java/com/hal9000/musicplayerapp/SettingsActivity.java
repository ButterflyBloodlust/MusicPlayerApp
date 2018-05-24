package com.hal9000.musicplayerapp;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.DocumentsContract;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

import java.io.File;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: ServiceSettings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">ServiceSettings
 * API Guide</a> for more information on developing a ServiceSettings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    private static boolean wereModified = false;
    private static boolean wasPathModified = false;
    public static boolean wereModified(){ return wereModified; }
    public static boolean wasPathModified(){ return wasPathModified; }
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            wereModified = true;
            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            }else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static Preference.OnPreferenceChangeListener sPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            wereModified = true;
            return true;
        }
    };

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        // listener implementation
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        wereModified = false;
        wasPathModified = false;

        getFragmentManager().beginTransaction().replace(android.R.id.content, new GeneralPreferenceFragment()).commit();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {  // TODO add custom toolbar with back button and handle it here
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        public static final String KEY_CHOOSE_MUSIC_DIR = "choose_music_directory";
        public static final String KEY_CONTINUOUS_PLAY = "continuous_play";
        public static final String KEY_SHUFFLE_PLAY = "shuffle_play";
        private static final int REQ_CODE_CHOOSE_MUSIC_DIR = 1;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);
/*
            Preference filePicker = findPreference(KEY_CHOOSE_MUSIC_DIR);
            filePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE"); //Intent to start openIntents File Manager
                    startActivityForResult(intent, REQ_CODE_CHOOSE_MUSIC_DIR);
                    return true;
                }
            });
*/
            CheckBoxPreference continuousPlay = (CheckBoxPreference) findPreference(KEY_CONTINUOUS_PLAY);
            continuousPlay.setOnPreferenceChangeListener(sPreferenceChangeListener);
            CheckBoxPreference shufflePlay = (CheckBoxPreference) findPreference(KEY_SHUFFLE_PLAY);
            shufflePlay.setOnPreferenceChangeListener(sPreferenceChangeListener);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            //bindPreferenceSummaryToValue(findPreference("sort_by_list"));
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch(requestCode) {
                case REQ_CODE_CHOOSE_MUSIC_DIR:
                    if (data != null && data.getData() != null) {
                        Log.i("Test", "Result URI " + data.getData().getPath());
                        Log.i("Test", "Result URI " + (new File(data.getData().getPath())).getAbsolutePath());
                        String newValue = data.getData().getPath();

                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(KEY_CHOOSE_MUSIC_DIR, newValue);
                        editor.apply();
                        wasPathModified = true;
                    }
                    break;
            }
        }
    }
}

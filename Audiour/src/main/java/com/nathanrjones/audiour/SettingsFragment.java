package com.nathanrjones.audiour;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.ShareCompat;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        Preference sharePref = findPreference("pref_share_app");

        if (sharePref == null) return;

        sharePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent shareIntent = ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(getString(R.string.app_store_url))
                        .getIntent();

                startActivity(shareIntent);
                return true;
            }
        });
    }

}
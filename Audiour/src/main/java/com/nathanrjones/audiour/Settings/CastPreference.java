package com.nathanrjones.audiour.settings;
/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.nathanrjones.audiour.AudiourApplication;
import com.nathanrjones.audiour.R;
import com.nathanrjones.audiour.utils.Utils;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class CastPreference extends PreferenceActivity
        implements OnSharedPreferenceChangeListener {

    public static final String APP_DESTRUCTION_KEY = "application_destruction";
    public static final String FTU_SHOWN_KEY = "ftu_shown";
    public static final String VOLUME_SELCTION_KEY = "volume_target";
    public static final String TERMINATION_POLICY_KEY = "termination_policy";
    public static final String STOP_ON_DISCONNECT = "1";
    public static final String CONTINUE_ON_DISCONNECT = "0";
    private ListPreference mVolumeListPreference;
    private SharedPreferences mPrefs;
    private VideoCastManager mCastManager;
    boolean mStopOnExit;
    private ListPreference mTerminationListPreference;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        getPreferenceScreen().getSharedPreferences().
                registerOnSharedPreferenceChangeListener(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mCastManager = AudiourApplication.getCastManager(this);

        // -- Termination Policy -------------------//
        mTerminationListPreference = (ListPreference) getPreferenceScreen().findPreference(
                TERMINATION_POLICY_KEY);
        mTerminationListPreference.setSummary(getTerminationSummary(mPrefs));
        mCastManager.setStopOnDisconnect(mStopOnExit);

        // -- Volume settings ----------------------//
        mVolumeListPreference = (ListPreference) getPreferenceScreen()
                .findPreference(VOLUME_SELCTION_KEY);
        String volValue = mPrefs.getString(
                VOLUME_SELCTION_KEY, getString(R.string.prefs_volume_default));
        String volSummary = getResources().getString(R.string.prefs_volume_title_summary, volValue);
        mVolumeListPreference.setSummary(volSummary);

        EditTextPreference versionPref = (EditTextPreference) findPreference("app_version");
        versionPref.setTitle(getString(R.string.version, Utils.getAppVersionName(this)));
    }

    public static boolean isDestroyAppOnDisconnect(Context ctx) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sharedPref.getBoolean(APP_DESTRUCTION_KEY, false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (VOLUME_SELCTION_KEY.equals(key)) {
            String value = sharedPreferences.getString(VOLUME_SELCTION_KEY, "");
            String summary = getResources().getString(R.string.prefs_volume_title_summary, value);
            mVolumeListPreference.setSummary(summary);
        } else if (TERMINATION_POLICY_KEY.equals(key)) {
            mTerminationListPreference.setSummary(getTerminationSummary(sharedPreferences));
            mCastManager.setStopOnDisconnect(mStopOnExit);
        }
    }

    private String getTerminationSummary(SharedPreferences sharedPreferences) {
        String valueStr = sharedPreferences.getString(TERMINATION_POLICY_KEY, "0");
        String[] labels = getResources().getStringArray(R.array.prefs_termination_policy_names);
        int value = CONTINUE_ON_DISCONNECT.equals(valueStr) ? 0 : 1;
        mStopOnExit = value == 0 ? false : true;
        return labels[value];
    }

    public static boolean isFtuShown(Context ctx) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sharedPref.getBoolean(FTU_SHOWN_KEY, false);
    }

    public static void setFtuShown(Context ctx) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        sharedPref.edit().putBoolean(FTU_SHOWN_KEY, true).commit();
    }

    @Override
    protected void onResume() {
        if (null != mCastManager) {
            mCastManager.incrementUiCounter();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (null != mCastManager) {
            mCastManager.decrementUiCounter();
        }
        super.onPause();
    }

}
package com.nathanrjones.audiour;

import com.nathanrjones.audiour.settings.CastPreference;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.utils.Utils;
import com.nathanrjones.audiour.settings.CastPreference;

import android.app.Application;
import android.content.Context;

/**
 * The {@link Application} for this demo application.
 */
public class AudiourApplication extends Application {
    private static String APPLICATION_ID;
    private static VideoCastManager mCastMgr = null;
    public static final double VOLUME_INCREMENT = 0.05;
    private static Context mAppContext;

    /*
     * (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = getApplicationContext();
        APPLICATION_ID = getString(R.string.app_id);
        Utils.saveFloatToPreference(getApplicationContext(),
                VideoCastManager.PREFS_KEY_VOLUME_INCREMENT, (float) VOLUME_INCREMENT);

    }

    public static VideoCastManager getCastManager(Context context) {
        if (null == mCastMgr) {
            mCastMgr = VideoCastManager.initialize(context, APPLICATION_ID,
                    null, null);
            mCastMgr.enableFeatures(
                    VideoCastManager.FEATURE_NOTIFICATION |
                            VideoCastManager.FEATURE_LOCKSCREEN |
                            VideoCastManager.FEATURE_DEBUGGING);

        }
        mCastMgr.setContext(context);
        String destroyOnExitStr = Utils.getStringFromPreference(context,
                CastPreference.TERMINATION_POLICY_KEY);
        mCastMgr.setStopOnDisconnect(null != destroyOnExitStr
                && CastPreference.STOP_ON_DISCONNECT.equals(destroyOnExitStr));
        return mCastMgr;
    }

}
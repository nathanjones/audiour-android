package com.nathanrjones.audiour;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.widget.Toast;

import com.google.cast.ApplicationChannel;
import com.google.cast.ApplicationMetadata;
import com.google.cast.ApplicationSession;
import com.google.cast.CastContext;
import com.google.cast.CastDevice;
import com.google.cast.ContentMetadata;
import com.google.cast.MediaProtocolMessageStream;
import com.google.cast.MediaRouteAdapter;
import com.google.cast.MediaRouteHelper;
import com.google.cast.MediaRouteStateChangeListener;
import com.google.cast.SessionError;

import java.io.IOException;

public class AudiourMediaRouteAdapter implements MediaRouteAdapter {

    private Context mContext;
    private CastContext mCastContext;
    private CastDevice mSelectedDevice;

    private MediaPlayer mMediaPlayer;

    private MediaRouter mMediaRouter;
    private MediaRouteButton mMediaRouteButton;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private MediaRouteStateChangeListener mRouteStateListener;

    private AudiourMedia mSelectedMedia;
    private ContentMetadata mAudiourMeta;

    private ApplicationSession mSession;
    private MediaProtocolMessageStream mMediaMessageStream;

    private ProgressDialog mProgressDialog;

    private String mAppName;
    private static final String NRJ_APP_NAME = "af2828a5-5a82-4be6-960a-2171287aed09";

    private static AudiourMediaRouteAdapter instance = null;

    public static AudiourMediaRouteAdapter getInstance(Context activity) {
        if (instance == null) {
            instance = new AudiourMediaRouteAdapter(activity);
        }
        return instance;
    }

    private AudiourMediaRouteAdapter(Context activity) {
        mContext = activity;

        mCastContext = new CastContext(mContext);
        mMediaRouter = MediaRouter.getInstance(mContext);

        MediaRouteHelper.registerMinimalMediaRouteProvider(mCastContext, this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mAppName = prefs.getString("pref_app_name", NRJ_APP_NAME);


        mMediaRouteSelector = MediaRouteHelper.buildMediaRouteSelector(
                MediaRouteHelper.CATEGORY_CAST,
                mAppName,
                null
        );

        mAudiourMeta = new ContentMetadata();
        mAudiourMeta.setTitle("Audiour - Share Audio, Simply");
        mAudiourMeta.setImageUrl(Uri.parse("http://audiour.com/favicon.ico"));

        mMediaRouterCallback = new MediaRouterCallback();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    public void setMediaRouteButtonSelector(MediaRouteButton mediaRouteButton) {
        mMediaRouteButton = mediaRouteButton;
        mMediaRouteButton.setRouteSelector(mMediaRouteSelector);
    }

    private class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            MediaRouteHelper.requestCastDeviceForRoute(route);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            try {
                if (mSession != null) {
                    mSession.setStopApplicationWhenEnding(true);
                    mSession.endSession();
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSelectedDevice = null;
            mMediaMessageStream = null;
        }
    }

    @Override
    public void onDeviceAvailable(CastDevice device, String s, MediaRouteStateChangeListener listener) {
        mSelectedDevice = device;
        mRouteStateListener = listener;

        Toast.makeText(mContext, "Connected to " + device.getFriendlyName(), Toast.LENGTH_SHORT).show();

        openSession();

    }

    private void openSession() {
        mSession = new ApplicationSession(mCastContext, mSelectedDevice);

        int flags = 0;

        mSession.setApplicationOptions(flags);

        mSession.setListener(new com.google.cast.ApplicationSession.Listener() {

            @Override
            public void onSessionStarted(ApplicationMetadata appMetadata) {

                Toast.makeText(mContext, "Session Started.", Toast.LENGTH_SHORT).show();

                ApplicationChannel channel = mSession.getChannel();

                if (channel == null) return;

                mMediaMessageStream = new MediaProtocolMessageStream();
                channel.attachMessageStream(mMediaMessageStream);

                if (mSelectedMedia != null) try {
                    mMediaMessageStream.loadMedia(mSelectedMedia.getUrl(), mAudiourMeta, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (mMediaPlayer != null){
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }
            }

            @Override
            public void onSessionStartFailed(SessionError sessionError) {
                Toast.makeText(mContext, "Session Start Failed.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSessionEnded(SessionError error) {
                Toast.makeText(mContext, "Session Ended.", Toast.LENGTH_LONG).show();
            }
        });

        try {
            Toast.makeText(mContext, "Starting Cast Keys Session", Toast.LENGTH_SHORT).show();
            mSession.startSession(NRJ_APP_NAME);
        } catch (IOException e) {
            Toast.makeText(mContext, "Failed to open session", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSetVolume(double volume) {
        if(mMediaMessageStream != null) try {
            mMediaMessageStream.setVolume(volume);
            mRouteStateListener.onVolumeChanged(volume);
        } catch (IllegalStateException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateVolume(double volume) {

    }

    public void play(){
        if(mMediaMessageStream != null) try {
            mMediaMessageStream.resume();
        } catch (IOException e) {
            e.printStackTrace();
        }
        else if (mMediaPlayer != null){
            mMediaPlayer.start();
        }
    }

    public void pause(){
        if(mMediaMessageStream != null) try {
            mMediaMessageStream.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
        else if (mMediaPlayer != null){
            mMediaPlayer.pause();
        }
    }

    public void stop(){

        mSelectedMedia = null;

        if (mMediaMessageStream != null) try {
            mMediaMessageStream.loadMedia("", mAudiourMeta);
        } catch (IOException e) {
            e.printStackTrace();
        }
        else if (mMediaPlayer != null){
            mMediaPlayer.stop();
        }
    }

    public void setSelectedMedia(AudiourMedia selected){

        mSelectedMedia = selected;

        String url = mSelectedMedia.getUrl();
        String title = mSelectedMedia.getTitle();

        mAudiourMeta.setTitle(title);

        if (mMediaPlayer != null){
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mMediaMessageStream != null) try {
            mMediaMessageStream.loadMedia(url, mAudiourMeta, true);

            //mPlayButton.setVisibility(View.GONE);
            //mPauseButton.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        else {

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            try {
                mMediaPlayer.setDataSource(url);

                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.setMessage("Loading selected file...");
                mProgressDialog.show();
//                mPlayButton.setVisibility(View.GONE);
//                mPauseButton.setVisibility(View.GONE);
            } catch (IOException e) {
//                mProgressBar.setVisibility(View.GONE);
                e.printStackTrace();
            }

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer player) {
                    mProgressDialog.dismiss();
//                    mPlayButton.setVisibility(View.GONE);
//                    mPauseButton.setVisibility(View.VISIBLE);
                    player.start();
                }
            });

            mMediaPlayer.prepareAsync();
        }
    }

    public void cleanup(){
        MediaRouteHelper.unregisterMediaRouteProvider(mCastContext);
        mCastContext.dispose();

        if (mSession != null) {
            try {
                if (!mSession.hasStopped()) {
                    mSession.endSession();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mSession = null;
    }

}

package com.nathanrjones.audiour;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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
import java.util.ArrayList;
import java.util.HashMap;

import static android.app.ActionBar.NAVIGATION_MODE_STANDARD;

public class MainActivity extends FragmentActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, MediaRouteAdapter {

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;

    private FragmentActivity mActivity;

    private CastContext mCastContext;
    private MediaRouteButton mMediaRouteButton;
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private CastDevice mSelectedDevice;
    private MediaRouteStateChangeListener mRouteStateListener;

    private ContentMetadata mAudiourMeta;

    private Button mLoadButton;
    private Button mPlayButton;
    private Button mPauseButton;
    private Button mStopButton;

    private ApplicationSession mSession;
    private MediaProtocolMessageStream mMediaMessageStream;

    private String mAppName;

    private static final String NRJ_APP_NAME = "af2828a5-5a82-4be6-960a-2171287aed09";

    private static final int POSITION_MAIN = 0;
    private static final int POSITION_TRENDING = 1;
    private static final int POSITION_RANDOM = 2;
    private static final int POSITION_SETTINGS = 3;
    private static final int POSITION_ABOUT = 4;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        mCastContext = new CastContext( getApplicationContext() );
        MediaRouteHelper.registerMinimalMediaRouteProvider( mCastContext, this );

        mMediaRouter = MediaRouter.getInstance( getApplicationContext() );
        mMediaRouteSelector = MediaRouteHelper.buildMediaRouteSelector( MediaRouteHelper.CATEGORY_CAST );
        mMediaRouterCallback = new MediaRouterCallback();

        mAudiourMeta = new ContentMetadata();
        mAudiourMeta.setTitle("Audiour - Share Audio, Simply");
        mAudiourMeta.setImageUrl(Uri.parse("http://audiour.com/favicon.ico"));
    }

    @Override
    protected void onStart() {
        super.onStart();

        mActivity = this;
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);

        mLoadButton = (Button) findViewById(R.id.load_button);
        mPlayButton = (Button) findViewById(R.id.play_button);
        mPauseButton = (Button) findViewById(R.id.pause_button);
        mStopButton = (Button) findViewById(R.id.stop_button);

        mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoadClicked();
            }
        });

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayClicked();
            }
        });

        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPauseClicked();
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStopClicked();
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mAppName = prefs.getString("pref_app_name", NRJ_APP_NAME);

        String url = getIntent().getStringExtra("url");

        if (url != null && !url.equals("")) {
            EditText input = (EditText) findViewById(R.id.audiour_url);
            input.setText(url);
        }
    }

    @Override
    protected void onStop() {
        mMediaRouter.removeCallback(mMediaRouterCallback);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        MediaRouteHelper.unregisterMediaRouteProvider(mCastContext);
        mCastContext.dispose();
        super.onDestroy();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        Fragment fragment;
        Intent intent;

        switch (position){

            case POSITION_MAIN:
                fragment = new MainFragment();
                break;
            case POSITION_TRENDING:
                intent = new Intent();
                intent.setClass(MainActivity.this, BrowsePopularActivity.class);
                startActivityForResult(intent, 0);
                return;
            case POSITION_RANDOM:
                fragment = new BrowseRandomFragment();
                break;
            case POSITION_SETTINGS:
                intent = new Intent();
                intent.setClass(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, 0);
                return;
            case POSITION_ABOUT:
                fragment = new AboutFragment();
                break;
            default:
                fragment = new MainFragment();
        }

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case POSITION_MAIN:
                mTitle = getString(R.string.title_home);
                break;
            case POSITION_TRENDING:
                mTitle = getString(R.string.title_trending);
                break;
            case POSITION_RANDOM:
                mTitle = getString(R.string.title_random);
                break;
            case POSITION_SETTINGS:
                mTitle = getString(R.string.title_settings);
                break;
            case POSITION_ABOUT:
                mTitle = getString(R.string.title_about);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        assert actionBar != null;
        actionBar.setNavigationMode(NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate( R.menu.main, menu );

            MenuItem mediaRouteItem = menu.findItem( R.id.action_cast );
            mMediaRouteButton = (MediaRouteButton) mediaRouteItem.getActionView();
            mMediaRouteButton.setRouteSelector( mMediaRouteSelector );

            restoreActionBar();
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, 0);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void onLoadClicked() {
        try {
            if (mMediaMessageStream != null) {

                EditText urlEditText = (EditText) findViewById(R.id.audiour_url);
                String audiourUrl = urlEditText.getText().toString();

                mMediaMessageStream.loadMedia(audiourUrl, mAudiourMeta, true);
            }
        } catch (IOException e) {
        }
    }

    public void onPlayClicked() {
        try {
            if (mMediaMessageStream != null) {
                mMediaMessageStream.resume();
            }
        } catch (IOException e) {
        }
    }

    public void onPauseClicked() {
        try {
            if (mMediaMessageStream != null) {
                mMediaMessageStream.stop();
            }
        } catch (IOException e) {
        }
    }

    public void onStopClicked() {
        try {
            if (mMediaMessageStream != null) {
                mMediaMessageStream.loadMedia("", mAudiourMeta);
            }
        } catch (IOException e) {
        }
    }

    private class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            MediaRouteHelper.requestCastDeviceForRoute(route);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            mSelectedDevice = null;
            mRouteStateListener = null;
        }
    }

    @Override
    public void onDeviceAvailable(CastDevice castDevice, String s, MediaRouteStateChangeListener mediaRouteStateChangeListener) {
        mSelectedDevice = castDevice;
        mRouteStateListener = mediaRouteStateChangeListener;

        String deviceName = castDevice.getFriendlyName();
        Toast.makeText(this, "Connected to " + deviceName, Toast.LENGTH_SHORT).show();

        openSession();
    }

    /**
     * Starts a new video playback session with the current CastContext and selected device.
     */
    private void openSession() {
        mSession = new ApplicationSession(mCastContext, mSelectedDevice);

        int flags = 0;

        mSession.setApplicationOptions(flags);

        mSession.setListener(new com.google.cast.ApplicationSession.Listener() {

            @Override
            public void onSessionStarted(ApplicationMetadata appMetadata) {

                Toast.makeText(mActivity, "Session Started.", Toast.LENGTH_SHORT).show();

                ApplicationChannel channel = mSession.getChannel();

                if (channel == null) return;

                mMediaMessageStream = new MediaProtocolMessageStream();
                channel.attachMessageStream(mMediaMessageStream);

                EditText editText = (EditText) findViewById(R.id.audiour_url);
                String audiourUrl = editText.getText().toString();


                try {
                    mMediaMessageStream.loadMedia(audiourUrl, mAudiourMeta, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSessionStartFailed(SessionError sessionError) {
                Toast.makeText(mActivity, "Session Start Failed.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSessionEnded(SessionError error) {
                Toast.makeText(mActivity, "Session Ended.", Toast.LENGTH_LONG).show();
            }
        });

        try {
            Toast.makeText(this, "Starting Cast Keys Session", Toast.LENGTH_SHORT).show();
            mSession.startSession(NRJ_APP_NAME);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to open session", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSetVolume(double volume) {
        try {
            if(mMediaMessageStream != null){
                mMediaMessageStream.setVolume(volume);
                mRouteStateListener.onVolumeChanged(volume);
            }
        } catch (IllegalStateException e){
        } catch (IOException e){
        }
    }

    @Override
    public void onUpdateVolume(double v) {

    }

    // Fragments

    public static class MainFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(POSITION_MAIN);
        }
    }

    public static class AboutFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_about, container, false);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(POSITION_ABOUT);
        }
    }

    public class BrowseRandomFragment extends Fragment {

        ArrayList randomFilesList = new ArrayList<HashMap<String, String>>();
        ListView listView;

        @Override
        public void onStart() {
            super.onStart();

            HashMap<String, String> file = new HashMap<String, String>();

            file.put("title", "Borderlands 2");
            file.put("url", "http://audiour.com/frglqrgg");

            randomFilesList.add(file);
            randomFilesList.add(file);
            randomFilesList.add(file);
            randomFilesList.add(file);

            listView = (ListView)findViewById(android.R.id.list);

            ListAdapter adapter = new SimpleAdapter(mActivity, randomFilesList,
                    R.layout.audiour_simple_list_item,
                    new String[] { "title", "url" },
                    new int[] { R.id.title,R.id.url }
            );

            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    Toast.makeText(mActivity, "You Clicked " + position , Toast.LENGTH_SHORT).show();

                }
            });

   }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            return inflater.inflate(R.layout.fragment_placeholder, container, false);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(POSITION_RANDOM);
        }
    }

    public static class TrendingFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_placeholder, container, false);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(POSITION_TRENDING);
        }
    }

}

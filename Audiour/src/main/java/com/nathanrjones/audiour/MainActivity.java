package com.nathanrjones.audiour;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.app.ActionBar.NAVIGATION_MODE_STANDARD;

public class MainActivity extends FragmentActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, MediaRouteAdapter {

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;

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

    private MediaPlayer mMediaPlayer;

    private ApplicationSession mSession;
    private MediaProtocolMessageStream mMediaMessageStream;

    private String mAppName;

    private static final String NRJ_APP_NAME = "af2828a5-5a82-4be6-960a-2171287aed09";

    private static final int POSITION_TRENDING = 0;
    private static final int POSITION_RANDOM = 1;
    private static final int POSITION_RECENTS = 2;

    // JSON Node names
    private static final String TAG_ID = "AudioFileId";
    private static final String TAG_TITLE = "Title";
    private static final String TAG_URL = "Mp3Url";

    List<AudiourMedia> mPopularList = new ArrayList<AudiourMedia>();
    List<AudiourMedia> mRandomList = new ArrayList<AudiourMedia>();
    List<AudiourMedia> mRecentList = new ArrayList<AudiourMedia>();

    private static final String URL_POPULAR = "http://audiour.com/Popular";
    private static final String URL_RANDOM = "http://audiour.com/Random";
    private static final String URL_RECENT = "http://audiour.com/RecentlyUploaded";

    AudiourMedia mSelectedMedia;

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

        SlidingUpPanelLayout layout = null;

        layout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        if (layout != null){
            layout.setShadowDrawable(getResources().getDrawable(R.drawable.above_shadow));
            layout.setAnchorPoint(0.3f);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        mAppName = prefs.getString("pref_app_name", NRJ_APP_NAME);


        mCastContext = new CastContext( getApplicationContext() );
        MediaRouteHelper.registerMinimalMediaRouteProvider( mCastContext, this );

        mMediaRouter = MediaRouter.getInstance( getApplicationContext() );
        mMediaRouteSelector = MediaRouteHelper.buildMediaRouteSelector(
                MediaRouteHelper.CATEGORY_CAST,
                mAppName,
                null
        );
        mMediaRouterCallback = new MediaRouterCallback();

        mAudiourMeta = new ContentMetadata();
        mAudiourMeta.setTitle("Audiour - Share Audio, Simply");
        mAudiourMeta.setImageUrl(Uri.parse("http://audiour.com/favicon.ico"));

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)){
            Uri data = intent.getData();
            String id = data.getPathSegments().get(0);

            onMediaSelected(new AudiourMedia(id, "Shared Audiour File", data.toString() + ".mp3"));
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

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

        List<AudiourMedia> audiourMediaList = new ArrayList<AudiourMedia>();

        switch (position){
            case POSITION_TRENDING:
                audiourMediaList = mPopularList;
                break;
            case POSITION_RANDOM:
                audiourMediaList = mRandomList;
                break;
            case POSITION_RECENTS:
                audiourMediaList = mRecentList;
                break;
        }

        Fragment fragment = new AudiourMediaListFragment(audiourMediaList, android.R.id.list, position);

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case POSITION_TRENDING:
                mTitle = getString(R.string.title_trending);
                break;
            case POSITION_RANDOM:
                mTitle = getString(R.string.title_random);
                break;
            case POSITION_RECENTS:
                mTitle = getString(R.string.title_recents);
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

    public void onMediaSelected(AudiourMedia selected){
        mSelectedMedia = selected;

        String url = mSelectedMedia.getUrl();
        String title = mSelectedMedia.getTitle();

        mAudiourMeta.setTitle(title);

        EditText urlEditText = (EditText) findViewById(R.id.audiour_url);
        urlEditText.setText(url);

        TextView selectedMediaText = (TextView) findViewById(R.id.selected_media);
        selectedMediaText.setText(title);

        LinearLayout layout = (LinearLayout) findViewById(R.id.media_control_panel);
        layout.setVisibility(View.VISIBLE);

        if (mMediaPlayer != null){
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mMediaMessageStream != null) {
            try {
                mMediaMessageStream.loadMedia(url, mAudiourMeta, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "Loading File", Toast.LENGTH_SHORT).show();

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            try {
                mMediaPlayer.setDataSource(url);
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer player) {
                    Toast.makeText(MainActivity.this, "Playing File", Toast.LENGTH_SHORT).show();
                    player.start();
                }
            });

            mMediaPlayer.prepareAsync();
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

                Toast.makeText(MainActivity.this, "Session Started.", Toast.LENGTH_SHORT).show();

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
                Toast.makeText(MainActivity.this, "Session Start Failed.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSessionEnded(SessionError error) {
                Toast.makeText(MainActivity.this, "Session Ended.", Toast.LENGTH_LONG).show();
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

    private class AsyncTaskParams {
        private String mUrl;
        private List<AudiourMedia> mAudiourMediaList;

        public AsyncTaskParams(String url, List<AudiourMedia> audiourMediaList){
            mUrl = url;
            mAudiourMediaList = audiourMediaList;
        }

        public String getUrl(){
            return mUrl;
        }

        public List<AudiourMedia> getList(){
            return mAudiourMediaList;
        }
    }

    private class RetrieveAudiourFilesTask extends AsyncTask<AsyncTaskParams, Void, JSONArray> {

        private String mUrl;
        private List<AudiourMedia> mAudiourMediaList;

        protected JSONArray doInBackground(AsyncTaskParams... params) {
            mUrl = params[0].getUrl();
            mAudiourMediaList = params[0].getList();

            JSONParser parser = new JSONParser();
            return parser.getJSONFromUrl(mUrl);
        }

        protected void onPostExecute(JSONArray results) {

            try {
                for(int i=0;i<results.length();i++)
                {
                    JSONObject file = results.getJSONObject(i);// Used JSON Object from Android

                    //Storing each Json in a string variable
                    String id = file.getString(TAG_ID);
                    String title = file.getString(TAG_TITLE);
                    String url = file.getString(TAG_URL);

                    mAudiourMediaList.add(new AudiourMedia(id, title, url));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            final ListView popularFilesListView = (ListView) findViewById(android.R.id.list);

            final ListAdapter listAdapter = new AudiourMediaArrayAdapter(
                    MainActivity.this,
                    R.layout.card_list_item,
                    mAudiourMediaList
            );

            popularFilesListView.setAdapter(listAdapter);

            popularFilesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    onMediaSelected(mAudiourMediaList.get(position));
                }
            });
        }
    }

    // Fragments

    public class AudiourMediaListFragment extends Fragment {

        private List<AudiourMedia> mAudiourMediaList = new ArrayList<AudiourMedia>();
        private ListView mListView;
        private int mListViewId;
        private int mMenuPosition;

        public AudiourMediaListFragment(List<AudiourMedia> mediaList, int listViewId, int menuPosition) {
            mAudiourMediaList = mediaList;
            mListViewId = listViewId;
            mMenuPosition = menuPosition;

            if (mAudiourMediaList.size() == 0) retrieveAudiourMediaList();
        }

        @Override
        public void onStart() {
            super.onStart();

            ListAdapter listAdapter = new AudiourMediaArrayAdapter(
                    MainActivity.this,
                    R.layout.card_list_item,
                    mAudiourMediaList
            );

            ListView popularFilesListView = (ListView) findViewById(mListViewId);

            popularFilesListView.setAdapter(listAdapter);
            popularFilesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    onMediaSelected(mAudiourMediaList.get(position));
                }
            });

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_placeholder, container, false);

            SlidingUpPanelLayout layout = null;

            if (rootView != null) {
                layout = (SlidingUpPanelLayout) rootView.findViewById(R.id.sliding_layout);
            }

            if (layout != null){
                layout.setShadowDrawable(getResources().getDrawable(R.drawable.above_shadow));
                layout.setAnchorPoint(0.3f);
            }

            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(mMenuPosition);
        }

        private void retrieveAudiourMediaList(){

            String url = "";

            switch (mMenuPosition) {
                case POSITION_TRENDING:
                    url = URL_POPULAR;
                    break;
                case POSITION_RANDOM:
                    url = URL_RANDOM;
                    break;
                case POSITION_RECENTS:
                    url = URL_RECENT;
                    break;
            }

            RetrieveAudiourFilesTask task = new RetrieveAudiourFilesTask();
            AsyncTaskParams asyncTaskParams = new AsyncTaskParams(url, mAudiourMediaList);
            task.execute(asyncTaskParams);
        }
    }


}

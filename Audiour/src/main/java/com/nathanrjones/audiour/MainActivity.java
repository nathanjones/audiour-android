package com.nathanrjones.audiour;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.MediaRouteButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

import static android.app.ActionBar.NAVIGATION_MODE_STANDARD;

public class MainActivity extends FragmentActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle = getTitle();

    private AudiourMediaRouteAdapter mAudiourMediaRouteAdapter;

    private ImageButton mPlayButton;
    private ImageButton mPauseButton;
    private ImageButton mStopButton;

    private MenuItem mShareItem;
    private ShareActionProvider mShareActionProvider;

    private ProgressBar mProgressBar;
    private PullToRefreshLayout mPullToRefreshLayout;

    private int mCurrentPosition;
    private static final int POSITION_FEATURED = 0;
    private static final int POSITION_TRENDING = 1;
    private static final int POSITION_RANDOM = 2;
    private static final int POSITION_RECENTS = 3;

    // JSON Node names
    private static final String TAG_ID = "AudioFileId";
    private static final String TAG_TITLE = "Title";
    private static final String TAG_URL = "Url";
    private static final String TAG_MP3_URL = "Mp3Url";

    private List<AudiourMedia> mCurrentList;
    private List<AudiourMedia> mFeaturedList = new ArrayList<AudiourMedia>();
    private List<AudiourMedia> mPopularList = new ArrayList<AudiourMedia>();
    private List<AudiourMedia> mRandomList = new ArrayList<AudiourMedia>();
    private List<AudiourMedia> mRecentList = new ArrayList<AudiourMedia>();

    private static final String URL_BASE = "http://api.audiour.com";
    private static final String URL_FEATURED = URL_BASE + "/Featured";
    private static final String URL_POPULAR = URL_BASE + "/Popular";
    private static final String URL_RANDOM = URL_BASE + "/Random";
    private static final String URL_RECENT = URL_BASE + "/Recent";

    public static final String PLAY_ACTION = "com.nathanrjones.audiour.playbackcommand.play";
    public static final String PAUSE_ACTION = "com.nathanrjones.audiour.playbackcommand.pause";
    public static final String STOP_ACTION = "com.nathanrjones.audiour.playbackcommand.stop";

    AudiourMedia mSelectedMedia;

    private int mNotifyId;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mNotifyBuilder;

    private MixpanelAPI mMixpanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(PLAY_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(STOP_ACTION);
        registerReceiver(mIntentReceiver, commandFilter);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        mPlayButton = (ImageButton) findViewById(R.id.play_button);
        mPauseButton = (ImageButton) findViewById(R.id.pause_button);
        mStopButton = (ImageButton) findViewById(R.id.stop_button);

        SlidingUpPanelLayout layout;

        layout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        if (layout != null){
            layout.setShadowDrawable(getResources().getDrawable(R.drawable.above_shadow));
            layout.setAnchorPoint(0.8f);
            layout.setHardAnchorPoint(true);
            layout.setEnableDragViewTouchEvents(true);
            layout.setCoveredFadeColorEnabled(false);
        }

        mAudiourMediaRouteAdapter = AudiourMediaRouteAdapter.getInstance(MainActivity.this);
    
        buildAppNotification();

        String mixpanelToken = getResources().getString(R.string.mixpanel_token);
        mMixpanel = MixpanelAPI.getInstance(this, mixpanelToken);

    }

    @Override
    protected void onStart() {
        super.onStart();

        mAudiourMediaRouteAdapter.setMediaRouterCallback();

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

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)){
            Uri data = intent.getData();
            if (data == null) return;

            JSONObject props = new JSONObject();
            try {
                props.put("Intent URL", data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mMixpanel.track("Started via Intent", props);

            List<String> segments = data.getPathSegments();
            if (segments == null) return;
            if (segments.isEmpty()) return;

            String id = segments.get(0);

            if (id.equals("popular")) {
                onNavigationDrawerItemSelected(POSITION_TRENDING);
            } else if (id.equals("random")) {
                onNavigationDrawerItemSelected(POSITION_RANDOM);
            } else if (id.equals("recent")) {
                onNavigationDrawerItemSelected(POSITION_RECENTS);
            } else {
                //onMediaSelected(new AudiourMedia(id, "Shared Audiour File", data.toString() + ".mp3"));
                RetrieveAudiourMetadataTask task = new RetrieveAudiourMetadataTask();
                task.execute("http://audiour.com/" + id);

            }
        }

        EasyTracker.getInstance(this).activityStart(this);

    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
        mAudiourMediaRouteAdapter.removeMediaRouterCallback();
    }

    @Override
    protected void onDestroy() {

        mNotifyManager.cancel(mNotifyId);

        mAudiourMediaRouteAdapter.cleanup();

        unregisterReceiver(mIntentReceiver);

        mMixpanel.flush();

        super.onDestroy();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, AudiourMediaListFragment.newInstance(position))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case POSITION_FEATURED:
                mTitle = getString(R.string.title_featured);
                break;
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

    public void onSectionStarted(int number){
        mCurrentPosition = number;

        final List<AudiourMedia> mediaList;

        switch (number) {
            case POSITION_FEATURED:
                mediaList = mFeaturedList;
                mMixpanel.track("Viewed Featured List", new JSONObject());
                break;
            case POSITION_TRENDING:
                mediaList = mPopularList;
                mMixpanel.track("Viewed Trending List", new JSONObject());
                break;
            case POSITION_RANDOM:
                mediaList = mRandomList;
                mMixpanel.track("Viewed Random List", new JSONObject());
                break;
            case POSITION_RECENTS:
                mediaList = mRecentList;
                mMixpanel.track("Viewed Recents List", new JSONObject());
                break;
            default:
                mediaList = new ArrayList<AudiourMedia>();
                break;
        }

        mCurrentList = mediaList;

        if (mediaList.size() == 0) {
            populateAudiourMediaList(number, mediaList);
        } else {
            ListAdapter listAdapter = new AudiourMediaArrayAdapter(
                    MainActivity.this,
                    R.layout.card_list_item,
                    mediaList
            );

            ListView listView = (ListView) findViewById(android.R.id.list);

            if (listView != null){
                listView.setAdapter(listAdapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        onMediaSelected(mediaList.get(position));
                    }
                });
            }
        }
    }

    public void onRefreshStarted(PullToRefreshLayout layout) {
        mPullToRefreshLayout = layout;

        mCurrentList = new ArrayList<AudiourMedia>();

        mMixpanel.track("Pulled to Refresh", new JSONObject());

        populateAudiourMediaList(mCurrentPosition, mCurrentList);
    }

    public void buildAppNotification() {

        mNotifyId = 1;

        String title = "Audiour";
        String text = "Share Audio, Simply.";

        if (mSelectedMedia != null) {
            title = mSelectedMedia.getTitle();
            text = mSelectedMedia.getId();
        }


            mNotifyBuilder = new NotificationCompat.Builder(MainActivity.this)
                .setSmallIcon(R.drawable.ic_chromecast_off)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle(title)
                .setContentText(text);

        BitmapDrawable bitmapIcon = (BitmapDrawable) getResources().getDrawable(R.drawable.ic_launcher);
        if (bitmapIcon  != null) mNotifyBuilder.setLargeIcon(bitmapIcon.getBitmap());

        Intent resultIntent = new Intent(MainActivity.this, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                MainActivity.this,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent playIntent = new Intent(PLAY_ACTION);
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 0, playIntent, 0);

        Intent pauseIntent = new Intent(PAUSE_ACTION);
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, 0, pauseIntent, 0);

        Intent stopIntent = new Intent(STOP_ACTION);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0);

        mNotifyBuilder.setContentIntent(resultPendingIntent);
        mNotifyBuilder.setDeleteIntent(stopPendingIntent);
        mNotifyBuilder.addAction(android.R.drawable.ic_media_play ,"Play", playPendingIntent);
        mNotifyBuilder.addAction(android.R.drawable.ic_media_pause ,"Pause", pausePendingIntent);
        mNotifyBuilder.addAction(android.R.drawable.ic_menu_delete ,"Stop", stopPendingIntent);

        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        assert actionBar != null;
        actionBar.setNavigationMode(NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);

        Drawable colorDrawable = new ColorDrawable(getResources().getColor(R.color.gray));
        Drawable bottomDrawable = getResources().getDrawable(R.drawable.actionbar_bottom);
        LayerDrawable layer = new LayerDrawable(new Drawable[] { colorDrawable, bottomDrawable });

        actionBar.setBackgroundDrawable(layer);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (!mNavigationDrawerFragment.isDrawerOpen()) {

            getMenuInflater().inflate( R.menu.main, menu );

            MenuItem mediaRouteItem = menu.findItem(R.id.action_cast);

            if (mediaRouteItem == null) return false;

            MediaRouteButton mediaRouteButton = (MediaRouteButton) mediaRouteItem.getActionView();

            if (mediaRouteButton != null){
                mediaRouteButton.setVisibility(View.GONE);
                mAudiourMediaRouteAdapter.setMediaRouteButtonSelector(mediaRouteButton);
            }

            mShareItem = menu.findItem(R.id.action_share);
            mShareActionProvider = (ShareActionProvider)
                    (mShareItem != null ? mShareItem.getActionProvider() : null);

            restoreActionBar();
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                mMixpanel.track("Opened Settings", new JSONObject());
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, 0);
                break;
            case R.id.action_search:
                mMixpanel.track("Opened Search Dialog", new JSONObject());
                onShowSearchDialog();
                break;
            case R.id.action_share:
                mMixpanel.track("Opened Share Dialog", new JSONObject());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onShowSearchDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.search_title));
        alert.setMessage(getString(R.string.search_prompt));

        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Open", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText() != null ? input.getText().toString() : "";

                if (!value.isEmpty()){
                    RetrieveAudiourMetadataTask task = new RetrieveAudiourMetadataTask();
                    task.execute("http://audiour.com/" + value);
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
            }
        });

        alert.show();
    }

    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    public void onPlayClicked() {
        mAudiourMediaRouteAdapter.play();

        mPlayButton.setVisibility(View.GONE);
        mPauseButton.setVisibility(View.VISIBLE);

        mMixpanel.track("Clicked Play", new JSONObject());
    }

    public void onPauseClicked() {
        mAudiourMediaRouteAdapter.pause();

        mPauseButton.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.VISIBLE);

        mMixpanel.track("Clicked Pause", new JSONObject());
    }

    public void onStopClicked() {
        mAudiourMediaRouteAdapter.stop();

        LinearLayout layout = (LinearLayout) findViewById(R.id.media_control_panel);
        layout.setVisibility(View.GONE);

        mShareItem.setVisible(false);

        mMixpanel.track("Clicked Stop", new JSONObject());
    }

    public void onMediaSelected(AudiourMedia selected){
        if (selected == null) return;

        mAudiourMediaRouteAdapter.setSelectedMedia(selected);

        mSelectedMedia = selected;

        String url = mSelectedMedia.getUrl();
        String title = mSelectedMedia.getTitle();

        TextView selectedMediaText = (TextView) findViewById(R.id.selected_media);
        if (selectedMediaText != null) selectedMediaText.setText(title);

        if (mPauseButton != null) mPauseButton.setVisibility(View.VISIBLE);
        if (mPlayButton != null) mPlayButton.setVisibility(View.GONE);

        LinearLayout layout = (LinearLayout) findViewById(R.id.media_control_panel);
        if (layout != null) layout.setVisibility(View.VISIBLE);

        if (mNotifyBuilder != null){
            mNotifyBuilder.setContentTitle(title);
            mNotifyBuilder.setContentText(url);
            mNotifyManager.notify(mNotifyId, mNotifyBuilder.build());
        }

        JSONObject props = new JSONObject();
        try {
            props.put("Selected Title", title);
            props.put("Selected URL", url);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mMixpanel.track("Selected File", props);

        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setText(selected.getUrl())
                .getIntent();

        setShareIntent(shareIntent);

        if (mShareItem != null) mShareItem.setVisible(true);
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (PLAY_ACTION.equals(action)) {
                onPlayClicked();
            } else if (PAUSE_ACTION.equals(action)) {
                onPauseClicked();
            } else if (STOP_ACTION.equals(action)) {
               onStopClicked();
            }
        }
    };

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

    private class RetrieveAudiourMetadataTask extends AsyncTask<String, Void, JSONObject> {

        private String mUrl;

        protected JSONObject doInBackground(String... params) {
            mUrl = params[0];

            JSONParser parser = new JSONParser();
            return parser.getObject(mUrl);
        }

        protected void onPostExecute(JSONObject result) {

            try {

                String id = result.getString(TAG_ID);
                String title = result.getString(TAG_TITLE);
                String url = result.getString(TAG_URL);
                String mp3Url = result.getString(TAG_MP3_URL);

                onMediaSelected(new AudiourMedia(id, title, url, mp3Url));

            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);

        }

    }

    private class RetrieveAudiourFilesTask extends AsyncTask<AsyncTaskParams, Void, JSONArray> {

        private String mUrl;
        private List<AudiourMedia> mAudiourMediaList;

        protected JSONArray doInBackground(AsyncTaskParams... params) {
            mUrl = params[0].getUrl();
            mAudiourMediaList = params[0].getList();

            JSONParser parser = new JSONParser();
            return parser.getArray(mUrl);
        }

        protected void onPostExecute(JSONArray results) {

            if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);

            if (mPullToRefreshLayout != null && mPullToRefreshLayout.isRefreshing()){
                mPullToRefreshLayout.setRefreshComplete();
            }

            if (results == null) {
                Toast.makeText(MainActivity.this, "Could not load files", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                for(int i=0;i<results.length();i++)
                {
                    JSONObject file = results.getJSONObject(i);// Used JSON Object from Android

                    //Storing each Json in a string variable
                    String id = file.getString(TAG_ID);
                    String title = file.getString(TAG_TITLE);
                    String url = file.getString(TAG_URL);
                    String mp3Url = file.getString(TAG_MP3_URL);

                    mAudiourMediaList.add(new AudiourMedia(id, title, url, mp3Url));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            final ListView listView = (ListView) findViewById(android.R.id.list);

            final ListAdapter listAdapter = new AudiourMediaArrayAdapter(
                    MainActivity.this,
                    R.layout.card_list_item,
                    mAudiourMediaList
            );

            if (listView != null){
                listView.setAdapter(listAdapter);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        onMediaSelected(mAudiourMediaList.get(position));
                    }
                });
            }
        }
    }

    private void populateAudiourMediaList(int currentPosition, List<AudiourMedia> mediaList){

        String url = "";

        switch (currentPosition) {
            case POSITION_FEATURED:
                url = URL_FEATURED;
                break;
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

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);

        RetrieveAudiourFilesTask task = new RetrieveAudiourFilesTask();
        AsyncTaskParams asyncTaskParams = new AsyncTaskParams(url, mediaList);
        task.execute(asyncTaskParams);
    }

}

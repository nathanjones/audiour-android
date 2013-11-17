package com.nathanrjones.audiour;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.MediaRouteButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.app.ActionBar.NAVIGATION_MODE_STANDARD;

public class MainActivity extends FragmentActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;

    private AudiourMediaRouteAdapter mAudiourMediaRouteAdapter;
    private MediaRouteButton mMediaRouteButton;

    private ImageButton mPlayButton;
    private ImageButton mPauseButton;
    private ImageButton mStopButton;

    private ProgressBar mProgressBar;

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
    private static final String URL_RECENT = "http://audiour.com/Recent";

    public static final String PLAY_ACTION = "com.nathanrjones.audiour.playbackcommand.play";
    public static final String PAUSE_ACTION = "com.nathanrjones.audiour.playbackcommand.pause";
    public static final String STOP_ACTION = "com.nathanrjones.audiour.playbackcommand.stop";

    AudiourMedia mSelectedMedia;

    private int mNotifyId;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mNotifyBuilder;


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
        mTitle = getTitle();

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        SlidingUpPanelLayout layout = null;

        layout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        if (layout != null){
            layout.setShadowDrawable(getResources().getDrawable(R.drawable.above_shadow));
            layout.setAnchorPoint(0.8f);
            layout.setHardAnchorPoint(true);
            layout.setEnableDragViewTouchEvents(true);
            layout.setCoveredFadeColorEnabled(false);
        }

        mAudiourMediaRouteAdapter = AudiourMediaRouteAdapter.getInstance(MainActivity.this);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)){
            Uri data = intent.getData();
            String id = data.getPathSegments().get(0);

//            onMediaSelected(new AudiourMedia(id, "Shared Audiour File", data.toString() + ".mp3"));
        }

        buildAppNotification();

    }

    @Override
    protected void onStart() {
        super.onStart();

//        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
//                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);

        mPlayButton = (ImageButton) findViewById(R.id.play_button);
        mPauseButton = (ImageButton) findViewById(R.id.pause_button);
        mStopButton = (ImageButton) findViewById(R.id.stop_button);

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
    }

    @Override
    protected void onDestroy() {

        mNotifyManager.cancel(mNotifyId);

        mAudiourMediaRouteAdapter.cleanup();

        unregisterReceiver(mIntentReceiver);

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
        final List<AudiourMedia> mediaList;

        switch (number) {
            case POSITION_TRENDING:
                mediaList = mPopularList;
                break;
            case POSITION_RANDOM:
                mediaList = mRandomList;
                break;
            case POSITION_RECENTS:
                mediaList = mRecentList;
                break;
            default:
                mediaList = new ArrayList<AudiourMedia>();
                break;
        }

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
            //.setLargeIcon((((BitmapDrawable) getResources().getDrawable(R.drawable.ic_launcher)).getBitmap()))
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentTitle(title)
            .setContentText(text);

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
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (!mNavigationDrawerFragment.isDrawerOpen()) {

            getMenuInflater().inflate( R.menu.main, menu );

            MenuItem mediaRouteItem = menu.findItem( R.id.action_cast );
            mMediaRouteButton = (MediaRouteButton) mediaRouteItem.getActionView();
            mAudiourMediaRouteAdapter.setMediaRouteButtonSelector(mMediaRouteButton);

            restoreActionBar();
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, 0);
                break;
            case R.id.action_about:
                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.fragment_about);
                dialog.setTitle("About Audiour Beta");
                dialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onPlayClicked() {
        mAudiourMediaRouteAdapter.play();

        mPlayButton.setVisibility(View.GONE);
        mPauseButton.setVisibility(View.VISIBLE);
    }

    public void onPauseClicked() {
        mAudiourMediaRouteAdapter.pause();

        mPauseButton.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.VISIBLE);
    }

    public void onStopClicked() {
        mAudiourMediaRouteAdapter.stop();

        LinearLayout layout = (LinearLayout) findViewById(R.id.media_control_panel);
        layout.setVisibility(View.GONE);
    }

    public void onMediaSelected(AudiourMedia selected){
        mAudiourMediaRouteAdapter.setSelectedMedia(selected);

        mSelectedMedia = selected;

        String url = mSelectedMedia.getUrl();
        String title = mSelectedMedia.getTitle();

        TextView selectedMediaText = (TextView) findViewById(R.id.selected_media);
        if (selectedMediaText != null) selectedMediaText.setText(title);

        mPauseButton.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.VISIBLE);

        LinearLayout layout = (LinearLayout) findViewById(R.id.media_control_panel);
        if (layout != null) layout.setVisibility(View.VISIBLE);

        if (mNotifyBuilder != null){
            mNotifyBuilder.setContentTitle(title);
            mNotifyBuilder.setContentText(url);
            mNotifyManager.notify(mNotifyId, mNotifyBuilder.build());
        }
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");

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

            if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);

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

    private void populateAudiourMediaList(int currentPosition, List<AudiourMedia> mediaList){

        String url = "";

        switch (currentPosition) {
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

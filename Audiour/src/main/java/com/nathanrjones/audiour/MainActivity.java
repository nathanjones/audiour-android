package com.nathanrjones.audiour;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.media.MediaRouter;
import android.util.Log;
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
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.google.gson.reflect.TypeToken;
import com.google.sample.castcompanionlibrary.cast.BaseCastManager;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.IVideoCastConsumer;
import com.google.sample.castcompanionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.sample.castcompanionlibrary.widgets.MiniController;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.nathanrjones.audiour.settings.CastPreference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

import static android.app.ActionBar.NAVIGATION_MODE_STANDARD;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, AudiourMediaArrayAdapter.Callback {

    private static final String PREF_STARRED_ITEMS = "STARRED_ITEMS";

    private static final String TAG = "MainActivity";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle = getTitle();

    private VideoCastManager mVideoCastManager;
    private IVideoCastConsumer mCastConsumer;
    private MiniController mMini;

    private MenuItem mShareItem;
    private ShareActionProvider mShareActionProvider;

    private ProgressBar mProgressBar;
    private PullToRefreshLayout mPullToRefreshLayout;

    private int mCurrentPosition;
    private static final int POSITION_FEATURED = 0;
    private static final int POSITION_TRENDING = 1;
    private static final int POSITION_RANDOM = 2;
    private static final int POSITION_RECENTS = 3;
    private static final int POSITION_STARRED = 4;

    private List<AudiourMedia> mCurrentList;
    private List<AudiourMedia> mFeaturedList = new ArrayList<AudiourMedia>();
    private List<AudiourMedia> mPopularList = new ArrayList<AudiourMedia>();
    private List<AudiourMedia> mRandomList = new ArrayList<AudiourMedia>();
    private List<AudiourMedia> mRecentList = new ArrayList<AudiourMedia>();
    private List<AudiourMedia> mStarredList = new ArrayList<AudiourMedia>();

    private Set<String> mStarredItems = new HashSet<String>();

    private static final String URL_BASE = "http://api.audiour.com";
    private static final String URL_FEATURED = URL_BASE + "/Featured";
    private static final String URL_POPULAR = URL_BASE + "/Popular";
    private static final String URL_RANDOM = URL_BASE + "/Random";
    private static final String URL_RECENT = URL_BASE + "/Recent";

    SharedPreferences mPreferences;
    SharedPreferences.Editor mPreferencesEditor;

    AudiourMedia mSelectedMedia;

    private MixpanelAPI mMixpanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BaseCastManager.checkGooglePlaySevices(this);

        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mVideoCastManager = AudiourApplication.getCastManager(this);

        mMini = (MiniController) findViewById(R.id.miniController);
        mVideoCastManager.addMiniController(mMini);

        mVideoCastManager.reconnectSessionIfPossible(this, true);

        mCastConsumer = new VideoCastConsumerImpl() {

            @Override
            public void onFailed(int resourceId, int statusCode) {

            }

            @Override
            public void onConnectionSuspended(int cause) {
                Log.d(TAG, "onConnectionSuspended() was called with cause: " + cause);
                com.nathanrjones.audiour.utils.Utils.
                        showToast(MainActivity.this, R.string.connection_temp_lost);
            }

            @Override
            public void onConnectivityRecovered() {
                com.nathanrjones.audiour.utils.Utils.
                        showToast(MainActivity.this, R.string.connection_recovered);
            }

            @Override
            public void onCastDeviceDetected(final MediaRouter.RouteInfo info) {
                if (!CastPreference.isFtuShown(MainActivity.this)) {
                    CastPreference.setFtuShown(MainActivity.this);

                    Log.d(TAG, "Route is visible: " + info);
                }
            }
        };

        String mixpanelToken = getResources().getString(R.string.mixpanel_token);
        mMixpanel = MixpanelAPI.getInstance(this, mixpanelToken);

    }

    @Override
    protected void onResume(){
        super.onResume();

        mVideoCastManager = AudiourApplication.getCastManager(this);
        if (null != mVideoCastManager) {
            mVideoCastManager.addVideoCastConsumer(mCastConsumer);
            mVideoCastManager.incrementUiCounter();
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        mVideoCastManager.decrementUiCounter();
        mVideoCastManager.removeVideoCastConsumer(mCastConsumer);
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();

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

            String id = segments.get(0).toLowerCase();

            if (id.equals("popular")) {
                onNavigationDrawerItemSelected(POSITION_TRENDING);
            } else if (id.equals("random")) {
                onNavigationDrawerItemSelected(POSITION_RANDOM);
            } else if (id.equals("recent")) {
                onNavigationDrawerItemSelected(POSITION_RECENTS);
            } else if (id.equals("starred")) {
                onNavigationDrawerItemSelected(POSITION_STARRED);
            } else {

                String baseUrl = getString(R.string.production_api_url);

                Ion.with(MainActivity.this, baseUrl + "/" + id)
                        .as(new TypeToken<AudiourMedia>() {})
                        .setCallback(new FutureCallback<AudiourMedia>() {
                            @Override
                            public void onCompleted(Exception e, AudiourMedia selectedMedia) {
                                if (selectedMedia != null) onMediaSelected(selectedMedia);
                            }
                        });
            }
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    @Override
    protected void onDestroy() {

        mVideoCastManager.removeMiniController(mMini);

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
            case POSITION_STARRED:
                mTitle = getString(R.string.title_starred);
                break;
        }
    }

    public void onSectionStarted(int number){

        mStarredItems = new HashSet<String>(mPreferences.getStringSet(PREF_STARRED_ITEMS, new HashSet<String>()));

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
            case POSITION_STARRED:
                mStarredList = new ArrayList<AudiourMedia>();
                mediaList = mStarredList;
                mMixpanel.track("Viewed Starred List", new JSONObject());
                break;
            default:
                mediaList = new ArrayList<AudiourMedia>();
                break;
        }

        mCurrentList = mediaList;

        if (mediaList.size() == 0) {
            populateAudiourMediaList(number, mediaList);
        } else {
            AudiourMediaArrayAdapter listAdapter = new AudiourMediaArrayAdapter(
                    MainActivity.this,
                    R.layout.card_list_item,
                    mediaList
            );
            listAdapter.setCallback(this);

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

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        assert actionBar != null;
        actionBar.setNavigationMode(NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);

        Drawable colorDrawable = new ColorDrawable(getResources().getColor(R.color.dark_gray));
        Drawable bottomDrawable = getResources().getDrawable(R.drawable.actionbar_bottom);
        LayerDrawable layer = new LayerDrawable(new Drawable[] { colorDrawable, bottomDrawable });

        actionBar.setBackgroundDrawable(layer);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (!mNavigationDrawerFragment.isDrawerOpen()) {

            getMenuInflater().inflate( R.menu.main, menu );

            mVideoCastManager.addMediaRouterButton(menu, R.id.action_cast);

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

                    String baseUrl = getString(R.string.production_api_url);

                    Ion.with(MainActivity.this, baseUrl + "/" + value)
                            .as(new TypeToken<AudiourMedia>() {})
                            .setCallback(new FutureCallback<AudiourMedia>() {
                                @Override
                                public void onCompleted(Exception e, AudiourMedia selectedMedia) {
                                    if (selectedMedia != null) onMediaSelected(selectedMedia);
                                }
                            });
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

    public void onMediaSelected(AudiourMedia selected){
        if (selected == null) return;

        mSelectedMedia = selected;

        String url = mSelectedMedia.Url;
        String title = mSelectedMedia.Title;

        MediaMetadata selectedMediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);

        selectedMediaMetadata.addImage(new WebImage(Uri.parse("http://audiour.com/favicon.ico")));
        selectedMediaMetadata.addImage(new WebImage(Uri.parse("http://natejon.es/images/audiour-splash.png")));
        selectedMediaMetadata.putString(MediaMetadata.KEY_TITLE, title);
        selectedMediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, url);

        MediaInfo selectedMediaInfo = new MediaInfo.Builder(selected.Mp3Url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("audio/mp3")
                .setMetadata(selectedMediaMetadata)
                .build();

        try {
            mVideoCastManager.loadMedia(selectedMediaInfo, true, 0);
        } catch (TransientNetworkDisconnectionException e) {
            e.printStackTrace();
        } catch (NoConnectionException e) {
            e.printStackTrace();
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
                .setSubject(selected.Title)
                .setText(selected.Url)
                .getIntent();

        setShareIntent(shareIntent);

        if (mShareItem != null) mShareItem.setVisible(true);
    }

    private void populateAudiourMediaList(int currentPosition, List<AudiourMedia> mediaList){

        if (currentPosition == POSITION_STARRED){

            mStarredList = new ArrayList<AudiourMedia>();

            ListView listView = (ListView) findViewById(android.R.id.list);

            if (listView == null) return;

            final AudiourMediaArrayAdapter listAdapter = new AudiourMediaArrayAdapter(
                    MainActivity.this,
                    R.layout.card_list_item,
                    mStarredList
            );
            listAdapter.setCallback(this);

            listView.setAdapter(listAdapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    onMediaSelected(mStarredList.get(position));
                }
            });

            for (String clypId: mStarredItems){

                Ion.with(MainActivity.this, URL_BASE + "/" + clypId)
                        .as(new TypeToken<AudiourMedia>(){})
                        .setCallback(new FutureCallback<AudiourMedia>() {

                            @Override
                            public void onCompleted(Exception e, AudiourMedia item) {
                                if (item != null) mStarredList.add(item);
                                listAdapter.notifyDataSetChanged();
                            }
                        });
            }

            if (mPullToRefreshLayout != null && mPullToRefreshLayout.isRefreshing()){
                mPullToRefreshLayout.setRefreshComplete();
            }

            return;
        }

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

        Ion.with(MainActivity.this, url)
                .as(new TypeToken<List<AudiourMedia>>() {})
                .setCallback(new FutureCallback<List<AudiourMedia>>() {

                    @Override
                    public void onCompleted(Exception e, List<AudiourMedia> list) {

                        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
                        if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);

                        if (mPullToRefreshLayout != null && mPullToRefreshLayout.isRefreshing()){
                            mPullToRefreshLayout.setRefreshComplete();
                         }

                        if (list == null) return;

                        final List<AudiourMedia> audiourList = list;
                        final ListView listView = (ListView) findViewById(android.R.id.list);
                        final AudiourMediaArrayAdapter listAdapter = new AudiourMediaArrayAdapter(
                                MainActivity.this,
                                R.layout.card_list_item,
                                audiourList
                        );

                        if (listView != null) {
                            listView.setAdapter(listAdapter);
                            listAdapter.setCallback(MainActivity.this);

                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    onMediaSelected(audiourList.get(position));
                                }
                            });
                        }
                    }
                });
    }

    @Override
    public void itemStarred(AudiourMedia item) {
        if (mStarredItems == null) mStarredItems = new HashSet<String>();

        if (item.Starred){
            mStarredItems.add(item.AudioFileId);
        } else {
            mStarredItems.remove(item.AudioFileId);
        }

        mPreferencesEditor = mPreferences.edit();
        mPreferencesEditor.putStringSet(PREF_STARRED_ITEMS, mStarredItems);
        mPreferencesEditor.commit();
    }
}

package com.nathanrjones.audiour;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.ShareCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by nathan on 1/15/14.
 */
public class AudiourMediaArrayAdapter extends ArrayAdapter<AudiourMedia> {

    private static final String PREF_STARRED_ITEMS = "STARRED_ITEMS";

    int mResource;

    SharedPreferences mPreferences;

    private String mBaseUrl;

    private Set<String> mStarredClyps = new HashSet<String>();

    private AudiourMediaArrayAdapter.Callback mCallback;

    public AudiourMediaArrayAdapter(Context context, int resource, List<AudiourMedia> items) {
        super(context, resource, items);
        mResource = resource;

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mStarredClyps = mPreferences.getStringSet(PREF_STARRED_ITEMS, new HashSet<String>());

        Boolean useStaging = mPreferences.getBoolean("pref_use_staging", false);
        mBaseUrl = useStaging ?
                getContext().getString(R.string.staging_api_url) :
                getContext().getString(R.string.production_api_url);
    }

    @Override
    public View getView(int index, View convertView, ViewGroup parent)
    {
        final ViewHolder viewHolder;

        if(convertView==null){
            String inflater = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater layoutInflater;
            layoutInflater = (LayoutInflater)getContext().getSystemService(inflater);
            convertView = layoutInflater.inflate(mResource, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.title = (TextView) convertView.findViewById(R.id.title);
            viewHolder.url = (TextView) convertView.findViewById(R.id.url);
            viewHolder.description = (TextView) convertView.findViewById(R.id.description);
            viewHolder.duration = (TextView) convertView.findViewById(R.id.duration);
            viewHolder.star = (ImageButton) convertView.findViewById(R.id.star);
            viewHolder.share = (ImageButton) convertView.findViewById(R.id.share);

            convertView.setTag(viewHolder);

        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.index = index;

        final AudiourMedia item = getItem(index);

        if (item != null) {
            viewHolder.title.setText(item.Title);
            viewHolder.url.setText(item.Url);

            String description = item.Description;
            viewHolder.description.setText(description);

            if (description != null && !description.isEmpty()) {
                viewHolder.description.setVisibility(View.VISIBLE);
            } else {
                viewHolder.description.setVisibility(View.GONE);
            }


            long totalSeconds = (long) Float.parseFloat(item.Duration);
            long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds);
            long seconds = totalSeconds - TimeUnit.MINUTES.toSeconds(minutes);

            String duration = "";

            if (minutes > 0) {
                duration = String.format("%dm %ds", minutes, seconds);
            } else {
                duration = String.format("%ds", seconds);
            }

            viewHolder.duration.setText(duration);

            if (mStarredClyps.contains(item.AudioFileId)) item.Starred = true;

            int starRes = item.Starred ? R.drawable.action_star_active : R.drawable.action_star;

            viewHolder.star.setFocusable(false);
            viewHolder.star.setTag(item.Title);
            viewHolder.star.setImageResource(starRes);
            viewHolder.star.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (item.Starred){
                        item.Starred = false;
                        viewHolder.star.setImageResource(R.drawable.action_star);
                    } else {
                        item.Starred = true;
                        viewHolder.star.setImageResource(R.drawable.action_star_active);
                    }

                    if (mCallback != null) mCallback.itemStarred(item);

                }
            });

            viewHolder.share.setFocusable(false);
            viewHolder.share.setTag(item.Url);
            viewHolder.share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = v.getContext();
                    String url = (String) v.getTag();

                    if (context == null) return;

                    Intent shareIntent = new Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, "Check out this Clyp")
                            .putExtra(Intent.EXTRA_TEXT, url);

                    context.startActivity(Intent.createChooser(shareIntent, "Share via"));

                }
            });
        }

        return convertView;
    }

    public void setCallback(Callback callback){
        mCallback = callback;
    }

    static class ViewHolder {
        TextView title;
        TextView description;
        TextView url;
        TextView duration;
        ImageButton star;
        ImageButton share;
        int index;
    }

    public interface Callback {

        public void itemStarred(AudiourMedia item);
    }
}

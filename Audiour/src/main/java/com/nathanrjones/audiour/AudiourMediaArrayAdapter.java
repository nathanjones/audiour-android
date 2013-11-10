package com.nathanrjones.audiour;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by nathan on 11/10/13.
 */
public class AudiourMediaArrayAdapter extends ArrayAdapter<AudiourMedia> {

    int mResource;

    public AudiourMediaArrayAdapter(Context context, int resource, List<AudiourMedia> items) {
        super(context, resource, items);
        mResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LinearLayout view;
        //Get the current alert object
        AudiourMedia item = getItem(position);

        //Inflate the view
        if (convertView == null) {
            view = new LinearLayout(getContext());
            String inflater = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater layoutInflater;
            layoutInflater = (LayoutInflater)getContext().getSystemService(inflater);
            layoutInflater.inflate(mResource, view, true);
        } else {
            view = (LinearLayout) convertView;
        }

        TextView title = (TextView) view.findViewById(R.id.title);
        TextView content = (TextView) view.findViewById(R.id.content);

        //Assign the appropriate data from our alert object above
        title.setText(item.getTitle());
        content.setText(item.getUrl());

        return view;
    }
}

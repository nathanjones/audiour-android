package com.nathanrjones.audiour;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NavigationDrawerAdapter extends ArrayAdapter<String> {

    int mResource;

    public NavigationDrawerAdapter(Context context, int resource, String[] items) {
        super(context, resource, items);
        mResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LinearLayout view;
        String item = getItem(position);

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

        title.setText(item);

        return view;
    }
}

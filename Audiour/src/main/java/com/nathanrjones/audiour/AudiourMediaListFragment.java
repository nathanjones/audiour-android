package com.nathanrjones.audiour;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class AudiourMediaListFragment extends Fragment implements OnRefreshListener  {

    private PullToRefreshLayout mPullToRefreshLayout;

    private static final String ARG_MENU_POSITION = "menu_position";

    public static AudiourMediaListFragment newInstance(int menuPosition) {
        AudiourMediaListFragment fragment = new AudiourMediaListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MENU_POSITION, menuPosition);
        fragment.setArguments(args);
        return fragment;
    }

    public AudiourMediaListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_placeholder, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view,savedInstanceState);
        ViewGroup viewGroup = (ViewGroup) view;

        mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());

        ActionBarPullToRefresh.from(getActivity())
                .insertLayoutInto(viewGroup)
                .allChildrenArePullable()
                .listener(this)
                .setup(mPullToRefreshLayout);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        int position = getArguments() != null ? getArguments().getInt(ARG_MENU_POSITION) : 0;

        ((MainActivity) activity).onSectionAttached(position);
    }

    @Override
    public void onStart() {
        super.onStart();

        MainActivity activity = (MainActivity) getActivity();
        int position = getArguments() != null ? getArguments().getInt(ARG_MENU_POSITION) : 0;

        if (activity != null) activity.onSectionStarted(position);
    }

    @Override
    public void onRefreshStarted(View view) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) activity.onRefreshStarted(mPullToRefreshLayout);
    }
}
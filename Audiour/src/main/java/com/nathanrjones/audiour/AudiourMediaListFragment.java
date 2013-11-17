package com.nathanrjones.audiour;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nathan on 11/17/13.
 */
public class AudiourMediaListFragment extends Fragment {

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_MENU_POSITION));
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).onSectionStarted(getArguments().getInt(ARG_MENU_POSITION));
    }

}
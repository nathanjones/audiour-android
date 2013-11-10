package com.nathanrjones.audiour;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.XMLReader;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class BrowsePopularActivity extends ListActivity {

    private static String url = "http://audiour.com/Popular";

    // JSON Node names
    private static final String TAG_ID = "AudioFileId";
    private static final String TAG_TITLE = "Title";
    private static final String TAG_URL = "Mp3Url";

    List<AudiourMedia> mPopularList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_placeholder);

        mPopularList = new ArrayList<AudiourMedia>();

        RetrievePopularFilesTask task = new RetrievePopularFilesTask();
        task.execute(url);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private HashMap<String, String> createFile(String id, String title, String url) {
        HashMap<String, String> file = new HashMap<String, String>();
        file.put(TAG_ID, id);
        file.put(TAG_TITLE, title);
        file.put(TAG_URL, url);
        return file;
    }

    private class RetrievePopularFilesTask extends AsyncTask<String, Void, JSONArray> {

        private Exception exception;

        protected JSONArray doInBackground(String... urls) {
            JSONParser parser = new JSONParser();
            return parser.getJSONFromUrl(url);
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

                    mPopularList.add(new AudiourMedia(id, title, url));
                }

                Toast.makeText(BrowsePopularActivity.this, "Trending List Loaded", Toast.LENGTH_SHORT).show();

                final ListView popularFilesListView = (ListView) findViewById(android.R.id.list);

                final ListAdapter listAdapter = new AudiourMediaArrayAdapter(
                        BrowsePopularActivity.this,
                        R.layout.card_list_item,
                        mPopularList
                );

                popularFilesListView.setAdapter(listAdapter);

                popularFilesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        AudiourMedia selected = mPopularList.get(position);

//                        Toast.makeText(BrowsePopularActivity.this, selected.getTitle(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(BrowsePopularActivity.this, selected.getUrl(), Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent();
                        intent.setClass(BrowsePopularActivity.this, MainActivity.class);
                        intent.putExtra("url", selected.getUrl());
                        startActivityForResult(intent, 0);
                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}

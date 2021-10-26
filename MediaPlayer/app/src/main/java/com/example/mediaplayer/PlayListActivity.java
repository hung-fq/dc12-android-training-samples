package com.example.mediaplayer;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class PlayListActivity extends ListActivity {

    public ArrayList<HashMap<String, String>> songsList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist);

        SongsManager songsListManager = new SongsManager();
        songsList = songsListManager.getPlayList();

        ArrayList<HashMap<String, String>> songsListData = new ArrayList<>(songsList);

        ListAdapter playlistAdapter = new SimpleAdapter(this, songsListData,
                R.layout.playlist_item, new String[]{"songTitle"}, new int[]{
                R.id.txtSongName});

        setListAdapter(playlistAdapter);

        ListView playlistView = getListView();
        playlistView.setOnItemClickListener((parent, view, position, id) -> {
            Intent songIntent = new Intent(getApplicationContext(), MusicPlayerActivity.class);
            songIntent.putExtra("songIndex", position);
            setResult(100, songIntent);
            finish();
        });
    }
}

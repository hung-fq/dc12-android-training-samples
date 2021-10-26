package com.example.mediaplayer;

import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;

public class SongsManager {

    final File MUSIC_FOLDER =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
    final ArrayList<HashMap<String, String>> songsList = new ArrayList<>();

    public ArrayList<HashMap<String, String>> getPlayList() {
        FileExtensionFilter fileExtensionFilter = new FileExtensionFilter();

        File[] mp3Files = MUSIC_FOLDER.listFiles(fileExtensionFilter);

        if (mp3Files != null && mp3Files.length > 0) {
            for (File file : mp3Files) {
                HashMap<String, String> song = new HashMap<>();
                song.put("songTitle", file.getName().substring(0, (file.getName().length() - 4)));
                song.put("songPath", file.getPath());

                songsList.add(song);
            }
        }
        return songsList;
    }

    static class FileExtensionFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith(".mp3") || name.endsWith(".MP3"));
        }
    }
}
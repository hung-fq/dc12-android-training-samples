package com.example.mediaplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;

public class MusicPlayerActivity extends Activity implements SeekBar.OnSeekBarChangeListener {

    private final static int PERMISSION_REQUEST = 1;
    public final static String ACTION_PLAY = "com.player.actionPlay";
    public final static String ACTION_PAUSE = "com.player.actionPause";
    public final static String ACTION_FORWARD = "com.player.actionForward";
    public final static String ACTION_REWIND = "com.player.actionRewind";
    public final static String ACTION_NEXT = "com.player.actionNext";
    public final static String ACTION_PREVIOUS = "com.player.actionPrevious";
    public final static String ACTION_SKIP = "com.player.actionSkip";
    public final static String ACTION_PLAY_FROM_LIST = "com.player.actionPlayFromList";

    private ArrayList<HashMap<String, String>> songsList;
    private ImageButton btnPlay;
    private SeekBar songProgressBar;
    private Utilities utils;
    private MusicPlayerState musicPlayerState;
    private MusicPlayerService myMusicPlayerService;
    private MusicPlayerServiceBound musicPlayerServiceBound = MusicPlayerServiceBound.NOT_BOUND;

    private int currentSongIndex = 0;

    private final Handler mediaHandler = new Handler();

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) service;
            myMusicPlayerService = binder.getService();
            musicPlayerServiceBound = MusicPlayerServiceBound.BOUND;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicPlayerServiceBound = MusicPlayerServiceBound.NOT_BOUND;
        }
    };

    private final Runnable updateTimeTask = new Runnable() {
        @SuppressLint("SetTextI18n")
        public void run() {
            TextView txtStartTime = findViewById(R.id.txtStartTime);
            TextView txtFinalTime = findViewById(R.id.txtFinalTime);
            long totalDuration = myMusicPlayerService.getTotalDuration();
            long currentDuration = myMusicPlayerService.getCurrentPosition();
            int progress = utils.getProgressPercentage(currentDuration, totalDuration);

            txtFinalTime.setText("" + utils.milliSecondsToTimer(totalDuration));
            txtStartTime.setText("" + utils.milliSecondsToTimer(currentDuration));
            songProgressBar.setProgress(progress);
            mediaHandler.postDelayed(this, 100);

            if (progress == 100) {
                btnPlay.setImageResource(R.drawable.btn_play_circle_filled);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_player);

        if (ContextCompat.checkSelfPermission(MusicPlayerActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MusicPlayerActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST);
        }

        Intent musicPlayerService = new Intent(MusicPlayerActivity.this,
                MusicPlayerService.class);
        startService(musicPlayerService);
        bindService(musicPlayerService, serviceConnection, Context.BIND_AUTO_CREATE);
        musicPlayerServiceBound = MusicPlayerServiceBound.BOUND;

        SongsManager songsManager = new SongsManager();
        songsList = songsManager.getPlayList();

        utils = new Utilities();
        btnPlay = findViewById(R.id.btnPlay);
        songProgressBar = findViewById(R.id.songProgressBar);
        songProgressBar.setOnSeekBarChangeListener(this);

        if (songsList.size() > 0) {
            onPlayButtonClick();
            onForwardButtonClick();
            onRewindButtonClick();
            onNextButtonClick();
            onPreviousButtonClick();
            initMusicPlayer(0);
        }
        onPlaylistButtonClick();
    }

    private void changeToBtnPause() {
        musicPlayerState = MusicPlayerState.PLAYING;
        btnPlay.setImageResource(R.drawable.btn_pause_circle_filled);
    }

    private void changeToBtnPlay() {
        musicPlayerState = MusicPlayerState.PAUSED;
        btnPlay.setImageResource(R.drawable.btn_play_circle_filled);
    }

    private void onPlayButtonClick() {
        btnPlay.setOnClickListener(arg0 -> {
            switch (musicPlayerState) {
                case PAUSED:
                    sendBroadcast(new Intent(ACTION_PLAY));
                    changeToBtnPause();
                    break;

                case PLAYING:
                    Intent actionPause = new Intent(ACTION_PAUSE);
                    actionPause.putExtra("currentSongIndex", currentSongIndex);
                    sendBroadcast(actionPause);
                    changeToBtnPlay();
                    break;

                default:
                    break;
            }
            makeButtonBlink(btnPlay);
        });
    }

    private void onForwardButtonClick() {
        ImageButton btnForward = findViewById(R.id.btnForward);
        btnForward.setOnClickListener(arg0 -> {
            sendBroadcast(new Intent(ACTION_FORWARD));
            makeButtonBlink(btnForward);
        });
    }

    private void onRewindButtonClick() {
        ImageButton btnRewind = findViewById(R.id.btnRewind);
        btnRewind.setOnClickListener(arg0 -> {
            sendBroadcast(new Intent(ACTION_REWIND));
            makeButtonBlink(btnRewind);
        });
    }

    private void onNextButtonClick() {
        ImageButton btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(arg0 -> {
            Intent actionNext = new Intent(ACTION_SKIP);
            currentSongIndex = currentSongIndex < (songsList.size() - 1) ? currentSongIndex + 1 : 0;
            actionNext.putExtra("currentSongIndex", currentSongIndex);
            sendBroadcast(actionNext);
            initMusicPlayer(currentSongIndex);
            makeButtonBlink(btnNext);
        });
    }

    private void onPreviousButtonClick() {
        ImageButton btnPrevious = findViewById(R.id.btnPrevious);
        btnPrevious.setOnClickListener(arg0 -> {
            Intent actionPrevious = new Intent(ACTION_SKIP);
            currentSongIndex = currentSongIndex > 0 ? currentSongIndex - 1 : songsList.size() - 1;
            actionPrevious.putExtra("currentSongIndex", currentSongIndex);
            sendBroadcast(actionPrevious);
            initMusicPlayer(currentSongIndex);
            makeButtonBlink(btnPrevious);
        });
    }

    private void onPlaylistButtonClick() {
        ImageButton btnPlaylist = findViewById(R.id.btnPlaylist);
        btnPlaylist.setOnClickListener(arg0 -> {
            Intent songIntent = new Intent(getApplicationContext(), PlayListActivity.class);
            startActivityForResult(songIntent, 100);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 100) {
            currentSongIndex = data.getExtras().getInt("songIndex");
            startPlaying(currentSongIndex);
        }
    }

    private void makeButtonBlink(ImageButton button) {
        Animation animation = new AlphaAnimation(0.5f, 1.0f);
        animation.setDuration(50);
        animation.setStartOffset(50);
        button.startAnimation(animation);
    }

    private void initMusicPlayer(int songIndex) {
        TextView txtSongName = findViewById(R.id.txtSongName);
        String txtSongTitle = songsList.get(songIndex).get("songTitle");
        txtSongName.setText(txtSongTitle);

        songProgressBar.setProgress(0);
        songProgressBar.setMax(100);
        mediaHandler.postDelayed(updateTimeTask, 100);

        changeToBtnPause();
    }

    private void startPlaying(int songIndex) {
        try {
            initMusicPlayer(songIndex);
            Intent actionPlayFromList = new Intent(ACTION_PLAY_FROM_LIST);
            actionPlayFromList.putExtra("songIndex", songIndex);
            sendBroadcast(actionPlayFromList);
        } catch (IllegalArgumentException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mediaHandler.removeCallbacks(updateTimeTask);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int totalDuration = myMusicPlayerService.getTotalDuration();
        int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);
        mediaHandler.removeCallbacks(updateTimeTask);
        myMusicPlayerService.seekToPosition(currentPosition);
        mediaHandler.postDelayed(updateTimeTask, 100);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (musicPlayerServiceBound == MusicPlayerServiceBound.BOUND) {
            unbindService(serviceConnection);
        }
    }
}
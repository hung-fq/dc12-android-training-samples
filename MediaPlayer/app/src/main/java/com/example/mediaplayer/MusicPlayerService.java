package com.example.mediaplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MusicPlayerService extends Service implements MediaPlayer.OnCompletionListener {

    private static final String channelId = "Music Player Channel";
    private static final int notificationId = 101;

    private final LocalBinder localBinder = new LocalBinder();
    private ArrayList<HashMap<String, String>> songsList = new ArrayList<>();
    private MediaPlayer mediaPlayer;

    private final BroadcastReceiver playSong = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            play();
        }
    };

    private final BroadcastReceiver pauseSong = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int currentSongIndex = intent.getExtras().getInt("currentSongIndex");
            pause();
            buildNotification(MusicPlayerState.PAUSED, currentSongIndex);
        }
    };

    private final BroadcastReceiver forwardSong = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            forward();
        }
    };

    private final BroadcastReceiver rewindSong = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            rewind();
        }
    };

    private final BroadcastReceiver skipSong = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int currentSongIndex = intent.getExtras().getInt("currentSongIndex");
            playFromList(currentSongIndex);
        }
    };

    private final BroadcastReceiver playSongFromList = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int songIndex = intent.getExtras().getInt("songIndex");
            playFromList(songIndex);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerToPlayFromList();
        registerToPlay();
        registerToPause();
        registerToForward();
        registerToRewind();
        registerToSkip();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        SongsManager songsManager = new SongsManager();

        mediaPlayer = new MediaPlayer();
        songsList = songsManager.getPlayList();

        if (songsList.size() != 0) {
            playFromList(0);
        }

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stop();
            stopSelf();
            mediaPlayer.release();
        }
        unregisterReceiver(playSong);
        unregisterReceiver(pauseSong);
        unregisterReceiver(forwardSong);
        unregisterReceiver(rewindSong);
        unregisterReceiver(skipSong);
        unregisterReceiver(playSongFromList);
        removeNotification();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stop();
        stopSelf();
    }

    private void registerToPlay() {
        IntentFilter intentFilter = new IntentFilter(MusicPlayerActivity.ACTION_PLAY);
        registerReceiver(playSong, intentFilter);
    }

    private void registerToPause() {
        IntentFilter intentFilter = new IntentFilter(MusicPlayerActivity.ACTION_PAUSE);
        registerReceiver(pauseSong, intentFilter);
    }

    private void registerToForward() {
        IntentFilter intentFilter = new IntentFilter(MusicPlayerActivity.ACTION_FORWARD);
        registerReceiver(forwardSong, intentFilter);
    }

    private void registerToRewind() {
        IntentFilter intentFilter = new IntentFilter(MusicPlayerActivity.ACTION_REWIND);
        registerReceiver(rewindSong, intentFilter);
    }

    private void registerToSkip() {
        IntentFilter intentFilter = new IntentFilter(MusicPlayerActivity.ACTION_SKIP);
        registerReceiver(skipSong, intentFilter);
    }

    private void registerToPlayFromList() {
        IntentFilter intentFilter = new IntentFilter(MusicPlayerActivity.ACTION_PLAY_FROM_LIST);
        registerReceiver(playSongFromList, intentFilter);
    }

    private void playFromList(int songIndex) {
        try {
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.reset();
            mediaPlayer.setDataSource(songsList.get(songIndex).get("songPath"));
            mediaPlayer.prepare();
            mediaPlayer.start();
            buildNotification(MusicPlayerState.PLAYING, songIndex);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void play() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    private void forward() {
        if (mediaPlayer != null) {
            int currentPosition = mediaPlayer.getCurrentPosition();
            int totalPosition = mediaPlayer.getDuration();
            int forwardTime = 5000;
            int forwardPosition = currentPosition + forwardTime;
            int seekToPosition = Math.min(forwardPosition, totalPosition);
            seekToPosition(seekToPosition);
        }
    }

    private void rewind() {
        if (mediaPlayer != null) {
            int currentPosition = mediaPlayer.getCurrentPosition();
            int rewindTime = 5000;
            int rewindPosition = currentPosition - rewindTime;
            int seekToPosition = Math.max(0, rewindPosition);
            seekToPosition(seekToPosition);
        }
    }

    private void stop() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    private void buildNotification(MusicPlayerState musicPlayerState, int songIndex) {
        int btnNotificationPlay = android.R.drawable.ic_media_play;
        int btnNotification = android.R.drawable.ic_media_pause;
        PendingIntent pendingNotificationAction =
                notificationAction(MusicPlayerNotificationAction.PAUSE);

        if (musicPlayerState == MusicPlayerState.PAUSED) {
            btnNotification = btnNotificationPlay;
            pendingNotificationAction = notificationAction(MusicPlayerNotificationAction.PLAY);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.music_player);
        NotificationCompat.Builder notificationBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this, channelId)
                        .setColor(getResources().getColor(R.color.primary_color))
                        .setLargeIcon(largeIcon)
                        .setContentText(songsList.get(songIndex).get("songTitle"))
                        .addAction(android.R.drawable.ic_media_previous, "previous",
                                notificationAction(MusicPlayerNotificationAction.PREVIOUS))
                        .addAction(btnNotification, "pause", pendingNotificationAction)
                        .addAction(android.R.drawable.ic_media_next, "next",
                                notificationAction(MusicPlayerNotificationAction.NEXT))
                        .setWhen(0)
                        .setPriority(Notification.PRIORITY_MAX);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(notificationId, notificationBuilder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    private PendingIntent notificationAction(MusicPlayerNotificationAction actionIntent) {
        Intent actionCode = new Intent(this, MusicPlayerService.class);
        switch (actionIntent) {
            case PLAY:
                actionCode.setAction(MusicPlayerActivity.ACTION_PLAY);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return PendingIntent.getService(this, actionIntent.ordinal(), actionCode,
                            PendingIntent.FLAG_IMMUTABLE);
                }

            case PAUSE:
                actionCode.setAction(MusicPlayerActivity.ACTION_PAUSE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return PendingIntent.getService(this, actionIntent.ordinal(), actionCode,
                            PendingIntent.FLAG_IMMUTABLE);
                }

            case NEXT:
                actionCode.setAction(MusicPlayerActivity.ACTION_NEXT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return PendingIntent.getService(this, actionIntent.ordinal(), actionCode,
                            PendingIntent.FLAG_IMMUTABLE);
                }

            case PREVIOUS:
                actionCode.setAction(MusicPlayerActivity.ACTION_PREVIOUS);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return PendingIntent.getService(this, actionIntent.ordinal(), actionCode,
                            PendingIntent.FLAG_IMMUTABLE);
                }

            default:
                break;

        }
        return null;
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public int getTotalDuration() {
        return mediaPlayer.getDuration();
    }

    public void seekToPosition(int position) {
        mediaPlayer.seekTo(position);
    }

    public class LocalBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }
}

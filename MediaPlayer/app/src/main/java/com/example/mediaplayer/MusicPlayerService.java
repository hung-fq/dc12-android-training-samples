package com.example.mediaplayer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
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
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MusicPlayerService extends Service implements MediaPlayer.OnCompletionListener {

    private int currentSongIndex;

    private static final String channelId = String.valueOf(R.string.channel_id);
    private static final int notificationId = R.string.notification_id;
    private final LocalBinder localBinder = new LocalBinder();

    private ArrayList<HashMap<String, String>> songsList = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) {
            return;
        }
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mediaSession = new MediaSessionCompat(getApplicationContext(), "MusicPlayer");
        transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                play();
                playFromList(currentSongIndex);
                sendBroadcast(new Intent(MusicPlayerActivity.ACTION_CHANGE_TO_BTN_PAUSE));
                buildNotification(MusicPlayerState.PLAYING, currentSongIndex);
            }

            @Override
            public void onPause() {
                super.onPause();
                pause();
                sendBroadcast(new Intent(MusicPlayerActivity.ACTION_CHANGE_TO_BTN_PLAY));
                buildNotification(MusicPlayerState.PAUSED, currentSongIndex);
                updateMetaData();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                currentSongIndex = currentSongIndex < (songsList.size() - 1) ?
                        currentSongIndex + 1 : 0;
                Intent initMusicPlayer = new Intent(MusicPlayerActivity.ACTION_INIT_MUSIC_PLAYER);
                initMusicPlayer.putExtra("currentSongIndex", currentSongIndex);

                playFromList(currentSongIndex);
                sendBroadcast(initMusicPlayer);
                sendBroadcast(new Intent(MusicPlayerActivity.ACTION_CHANGE_TO_BTN_PAUSE));
                buildNotification(MusicPlayerState.PLAYING, currentSongIndex);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                currentSongIndex = currentSongIndex > 0 ? currentSongIndex - 1 :
                        songsList.size() - 1;
                Intent initMusicPlayer = new Intent(MusicPlayerActivity.ACTION_INIT_MUSIC_PLAYER);
                initMusicPlayer.putExtra("currentSongIndex", currentSongIndex);

                playFromList(currentSongIndex);
                sendBroadcast(initMusicPlayer);
                sendBroadcast(new Intent(MusicPlayerActivity.ACTION_CHANGE_TO_BTN_PAUSE));
                buildNotification(MusicPlayerState.PLAYING, currentSongIndex);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                stopSelf();
            }
        });
    }

    private final BroadcastReceiver playSong = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            play();
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

    private final BroadcastReceiver pauseSong = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentSongIndex = intent.getExtras().getInt("currentSongIndex");
            pause();
            buildNotification(MusicPlayerState.PAUSED, currentSongIndex);
            updateMetaData();
        }
    };

    private final BroadcastReceiver skipSong = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentSongIndex = intent.getExtras().getInt("currentSongIndex");
            playFromList(currentSongIndex);
        }
    };

    private final BroadcastReceiver playSongFromList = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentSongIndex = intent.getExtras().getInt("songIndex");
            playFromList(currentSongIndex);
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

        handleIncomingActions(intent);
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
            stop();
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.reset();
            mediaPlayer.setDataSource(songsList.get(songIndex).get("songPath"));
            mediaPlayer.prepare();
            mediaPlayer.start();
            initMediaSession();
            buildNotification(MusicPlayerState.PLAYING, songIndex);
            updateMetaData();

        } catch (RemoteException | IOException e) {
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
                .setStyle(
                    new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2)
                    )
                .setColor(getResources().getColor(R.color.primary_color))
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setContentText(songsList.get(songIndex).get("songTitle"))
                .addAction(android.R.drawable.ic_media_previous, "previous",
                    notificationAction(MusicPlayerNotificationAction.PREVIOUS))
                .addAction(btnNotification, "pause", pendingNotificationAction)
                .addAction(android.R.drawable.ic_media_next, "next",
                    notificationAction(MusicPlayerNotificationAction.NEXT))
                .setWhen(0)
                .setPriority(Notification.PRIORITY_MAX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(notificationId, notificationBuilder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private PendingIntent notificationAction(MusicPlayerNotificationAction actionCode) {
        Intent actionIntent = new Intent(this, MusicPlayerService.class);
        switch (actionCode) {
            case PLAY:
                actionIntent.setAction(MusicPlayerActivity.ACTION_PLAY);
                return PendingIntent.getService(this, actionCode.ordinal(), actionIntent, 0);

            case PAUSE:
                actionIntent.setAction(MusicPlayerActivity.ACTION_PAUSE);
                return PendingIntent.getService(this, actionCode.ordinal(), actionIntent, 0);

            case NEXT:
                actionIntent.setAction(MusicPlayerActivity.ACTION_NEXT);
                return PendingIntent.getService(this, actionCode.ordinal(), actionIntent, 0);

            case PREVIOUS:
                actionIntent.setAction(MusicPlayerActivity.ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionCode.ordinal(), actionIntent, 0);

            default:
                break;
        }
        return null;
    }

    private void updateMetaData() {
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), R.mipmap.music_player);
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, songsList.get(currentSongIndex).get("songTitle"))
            .build());
    }

    private void handleIncomingActions(Intent actionIntent) {
        if (actionIntent == null || actionIntent.getAction() == null) return;
        String actionString = actionIntent.getAction();
        if (actionString.equalsIgnoreCase(MusicPlayerActivity.ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(MusicPlayerActivity.ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(MusicPlayerActivity.ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(MusicPlayerActivity.ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        }
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

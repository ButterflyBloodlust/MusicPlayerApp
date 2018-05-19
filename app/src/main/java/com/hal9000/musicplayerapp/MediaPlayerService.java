package com.hal9000.musicplayerapp;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.hal9000.musicplayerapp.MainActivity.LOG_ERR_TAG;


public class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener{
    private final IBinder musicBind = new MusicBinder();
    private MediaPlayer mMediaPlayer = null;
    int songPos;
    private final String CHANNEL_ID = "notification_channel_id";
    private final int NOW_PLAYING_NOTIFICATION_ID = 1;
    public static final String LOG_FILES_TAG = "FilesX";
    NotificationManagerCompat notificationManager;
    NotificationCompat.Builder notificationBuilder;
    boolean isPlayerPaused = false;
    boolean isPlayerStopped = true;
    public static final String NOW_PLAYING = "Now playing";
    public static final String PLAYER_PAUSED = "Player paused";

    private Map<Activity, IListenerFunctions> clients = new ConcurrentHashMap<Activity, IListenerFunctions>();

    public void onCreate(){
        //create the service
        super.onCreate();
        //initialize position
        songPos=0;
        //create player
        //mMediaPlayer = new MediaPlayer();
        //initialize
        //initMusicPlayer();
        //Toast.makeText(MediaPlayerService.this, "Service started", Toast.LENGTH_SHORT).show();
        showLocationNotification();
    }

    private void showLocationNotification()
    {
        notificationManager = NotificationManagerCompat.from(this);

        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Media player started")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(false);

        notificationManager.notify(NOW_PLAYING_NOTIFICATION_ID, notificationBuilder.build());

        startForeground(NOW_PLAYING_NOTIFICATION_ID, notificationBuilder.build());
    }

    private void changeNotificationContent(String path){
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        String temp = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        notificationBuilder.setContentTitle(temp);
        temp = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        notificationBuilder.setContentText(temp);
        notificationBuilder.setContentInfo(NOW_PLAYING);
        notificationManager.notify(NOW_PLAYING_NOTIFICATION_ID, notificationBuilder.build());
    }

    private void changeNotificationContentOnPaused(boolean isPaused){
        if (isPaused)
            notificationBuilder.setContentInfo(PLAYER_PAUSED);
        else
            notificationBuilder.setContentInfo(NOW_PLAYING);
        notificationManager.notify(NOW_PLAYING_NOTIFICATION_ID, notificationBuilder.build());
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(MediaPlayerService.this, "Service started", Toast.LENGTH_SHORT).show();    // don't use - causes 'Screen overlay detected popup when user is prompted for permissions
        Log.d(LOG_ERR_TAG, "Service started");
        initMediaPlayer();

        return START_NOT_STICKY;
    }

    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
    }

    public int getPlayerDuration(){
        return mMediaPlayer == null ? -1 : mMediaPlayer.getDuration();
    }

    public int getCurrentPlayerPosition(){
        return mMediaPlayer == null ? -1 : mMediaPlayer.getCurrentPosition();
    }

    /** Called when MediaPlayer is ready */
    public void onPrepared(MediaPlayer player) {
        player.start();
        isPlayerStopped = false;
        for (Activity client : clients.keySet()) {
            clients.get(client).setSeekBarMaxDuration(player.getDuration());
            Log.d(LOG_ERR_TAG, "onPrepared(): player.getDuration() = " + player.getDuration());
        }
    }

    public void playSong(String path){

        if (isPlayerPaused){
            mMediaPlayer.start();
            isPlayerPaused = false;
            changeNotificationContentOnPaused(false);
        }
        else {

            if (mMediaPlayer == null) {
                initMediaPlayer();
            }
            mMediaPlayer.reset();
            Uri musicUri = Uri.parse(path);
            try {
                mMediaPlayer.setDataSource(getApplicationContext(), musicUri);
            } catch (IOException e) {
                Toast.makeText(MediaPlayerService.this, "Invalid file path", Toast.LENGTH_SHORT).show();
                //stopSelf();
                return;
            }

            mMediaPlayer.prepareAsync(); // prepare async to not block main thread

            changeNotificationContent(path);
        }

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        isPlayerStopped = true;
    }

    public void pausePlaying(){
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            isPlayerPaused = true;
            changeNotificationContentOnPaused(true);
        }
    }

    public boolean isPlaying(){
        return !isPlayerPaused && !isPlayerStopped;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    //binder
    public class MusicBinder extends Binder{
        MediaPlayerService getService() {
            return MediaPlayerService.this;
        }

        public void registerActivity(Activity activity, IListenerFunctions callback){
            clients.put(activity, callback);
        }

        public void unregisterActivity(Activity activity) {
            clients.remove(activity);
        }
    }

    //release resources when unbind
    @Override
    public boolean onUnbind(Intent intent){
        Toast.makeText(MediaPlayerService.this, "Unbound", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopAndClearPlayer();
        stopSelf();
    }

    public void moveForward(int msec){
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()){
            mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() + msec);
        }
    }

    public void stopAndClearPlayer() {
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    /*
    public static void start(Context context, String fileName) {
        Intent starter = new Intent(context, MediaPlayerService.class);
        starter.putExtra(KEY_FILE_NAME, fileName);
        context.startService(starter);
    }
*/
    public static void stop(Context context){
        context.stopService(new Intent(context, MediaPlayerService.class));
    }

}

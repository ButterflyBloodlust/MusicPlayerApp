package com.hal9000.musicplayerapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import static android.os.Environment.DIRECTORY_MUSIC;
import static com.hal9000.musicplayerapp.MainActivity.LOG_ERR_TAG;


public class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener{
    private final IBinder musicBind = new MusicBinder();
    private MediaPlayer mMediaPlayer = null;
    int songPos;
    private final String CHANNEL_ID = "notification_channel_id";
    private final int NOW_PLAYING_NOTIFICTATION_ID = 1;
    public static final String LOG_FILES_TAG = "FilesX";
    NotificationManagerCompat notificationManager;
    NotificationCompat.Builder notificationBuilder;
    boolean isPlayerPaused = false;
    boolean isPlayerStopped = true;

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
                .setContentTitle("Title")
                .setContentText("Now playing")
                .setSmallIcon(R.mipmap.ic_launcher);

        notificationManager.notify(NOW_PLAYING_NOTIFICTATION_ID, notificationBuilder.build());

        startForeground(NOW_PLAYING_NOTIFICTATION_ID, notificationBuilder.build());
    }

    private void changeNotificationTitle(String title){
        notificationBuilder.setContentTitle(title);
        notificationManager.notify(NOW_PLAYING_NOTIFICTATION_ID, notificationBuilder.build());
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(MediaPlayerService.this, "Service started", Toast.LENGTH_SHORT).show();
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

    /** Called when MediaPlayer is ready */
    public void onPrepared(MediaPlayer player) {
        player.start();
        isPlayerStopped = false;
    }

    public void playSong(String path){

        if (isPlayerPaused){
            mMediaPlayer.start();
            isPlayerPaused = false;
            return;
        }

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
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        isPlayerStopped = true;
    }

    public void pausePlaying(){
        mMediaPlayer.pause();
        isPlayerPaused = true;
    }

    public boolean isPlaying(){
        return !isPlayerPaused && !isPlayerStopped;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    //binder
    public class MusicBinder extends Binder {
        MediaPlayerService getService() {
            return MediaPlayerService.this;
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

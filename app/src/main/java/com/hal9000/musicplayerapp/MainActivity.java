package com.hal9000.musicplayerapp;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;

import static com.hal9000.musicplayerapp.MediaPlayerService.LOG_FILES_TAG;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> songsPaths = null;

    // Requesting permission to READ_EXTERNAL_STORAGE
    private boolean permissionToReadExtStorageAccepted = false;
    private String [] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION_ID = 100;
    private static final String KEY_SONGS_PATHS = "songs_paths";
    private static final String KEY_LEFT_WHILE_PLAYING = "left_while_playing";
    public static final String KEY_SERVICE = "KEY_SERVICE";
    public static final String KEY_PLAY_INTENT = "KEY_PLAY_INTENT";
    public static final String LOG_ERR_TAG = "Err";

    //song list variables
    //private ArrayList<Song> songList;
    //private ListView songView;
    private ToggleButton playPauseButton = null;
    private ImageButton fastRewindButton = null;
    private ImageButton fastForwardButton = null;
    private TextView currentSongTitle = null;
    private SeekBar mSeekBar;

    //service
    private MediaPlayerService mService;
    private Intent playIntent;
    MediaPlayerService.MusicBinder binder;
    //binding
    private boolean musicBound = false;

    // Restore media player state info
    private boolean leftWhilePlaying = false;   // TODO implement one-time initialization class for this field (field should be read-only after first initialization not counting this line)
    private boolean playButtonUnusualState;

    private Handler requestHandler;
    private Runnable mRunnable;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_ERR_TAG, "onCreate() begin ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        restoreMediaPlayerStateInfo();
        if (playIntent==null)
            bindAndStartSrv();

        ActivityCompat.requestPermissions(this, permissions, REQUEST_READ_EXTERNAL_STORAGE_PERMISSION_ID);  // This call is asynchronous !!!

        requestHandler = new Handler(Looper.getMainLooper());

        initToolbar();
        initPlayPauseButton();
        restorePlayPauseButtonState();
        initTrackNavButtons();
        currentSongTitle = findViewById(R.id.textView_now_playing_title);
        initializeSeekBar();

        Log.d(LOG_ERR_TAG, "onCreate() end ");
    }

    private void initializeSeekBar() {
        mSeekBar = findViewById(R.id.seek_bar);

        // Set a change listener for seek bar
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int newProgress, boolean isFromUser) {   // newProgress is in seconds
                if(mService != null && isFromUser){
                    if(mService.isPlayerInitialized())
                        mService.setPlayerPosition(newProgress*1000);
                    else
                        seekBar.setProgress(0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    protected void initializeSeekBarRunnable(){
        Log.d(LOG_ERR_TAG, "Inside initializeSeekBarRunnable: ");
        mRunnable = new Runnable() {
            @Override
            public void run() {
                //Log.d(LOG_ERR_TAG, "Inside runnable: ");
                if(mService!=null){

                    int mCurrentPosition = mService.getCurrentPlayerPosition()/1000; // In milliseconds
                    mSeekBar.setProgress(mCurrentPosition);
                    //currentSongTitle.setText('a');
                }
                requestHandler.postDelayed(mRunnable,250);
            }
        };
        requestHandler.post(mRunnable);
    }

    private void initTrackNavButtons() {
        fastRewindButton = findViewById(R.id.imageButton_fast_rewind);
        fastRewindButton.setOnClickListener(new ImageButton.OnClickListener(){
            @Override
            public void onClick(View v){
                mService.moveForward(-10000); // +10 sec
            }
        });

        fastForwardButton = findViewById(R.id.imageButton_fast_forward);
        fastForwardButton.setOnClickListener(new ImageButton.OnClickListener(){
            @Override
            public void onClick(View v){
                mService.moveForward(10000); // +10 sec
            }
        });
    }

    private void restoreMediaPlayerStateInfo(){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        playButtonUnusualState = leftWhilePlaying = sharedPref.getBoolean(KEY_LEFT_WHILE_PLAYING, false);
    }

    private void processMusicFiles() {
        File[] files = getExternalFilesDirs(Environment.MEDIA_MOUNTED);
        String folderPath = "";
        for (int i = files.length - 1; i >= 0 && folderPath.length() == 0; i--){
            String tempStr = files[i].getAbsolutePath();
            tempStr = tempStr.substring(0, tempStr.indexOf("/Android/")) + "/Music/";
            //Log.d(LOG_FILES_TAG, "Path_temp: " + tempStr);
            if (new File(tempStr).exists())
                folderPath = tempStr;
        }
        //Log.d(LOG_FILES_TAG, "Path: " + folderPath);
/*  TODO Do below in recyclerview adapter
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        String authorName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        */
        File file = new File(folderPath);
        files = file.listFiles();
        int songsCount = getFilesCount(file);
        songsPaths = new ArrayList<String>(songsCount+50);
        addSongs(file);

        Log.d(LOG_FILES_TAG, "Size: " + files.length);
        for (int i=0; i<songsPaths.size(); i++){
            String path = songsPaths.get(i);
            Log.d(LOG_FILES_TAG, i + ": File path: " + path);
        }
    }

    public int getFilesCount(File dir) {
        File[] files = dir.listFiles();
        int count = 0;
        for (File f : files)
            if (f.isDirectory())
                count += getFilesCount(f);
            else
                count++;

        return count;
    }

    public void addSongs(File dir) {
        File[] files = dir.listFiles();
        for (File f : files)
            if (f.isDirectory())
                getFilesCount(f);
            else
                songsPaths.add(f.getAbsolutePath());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_READ_EXTERNAL_STORAGE_PERMISSION_ID:
                permissionToReadExtStorageAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToReadExtStorageAccepted ) finish();
        else {
            restoreState();
        }
    }

    private void initPlayPauseButton() {
        playPauseButton = findViewById(R.id.toggleButton_play_pause);
        playPauseButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked){
                    //Toast.makeText(MainActivity.this, "Service not started", Toast.LENGTH_SHORT).show();

                    if (playButtonUnusualState) {  // service was running & playing before activity was launched
                        playButtonUnusualState = false;
                    }
                    else if (songsPaths == null){
                        playPauseButton.setChecked(!isChecked);
                        Toast.makeText(MainActivity.this, "Song not yet loaded", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    else if (mService != null)
                        mService.playSong(songsPaths.get(595));
                    initializeSeekBarRunnable();
                }
                else
                {
                    //Toast.makeText(MainActivity.this, "Service not stopped", Toast.LENGTH_SHORT).show();
                    if (playButtonUnusualState){
                        playButtonUnusualState = false;
                    }
                    else if (mService != null) {
                        mService.pausePlaying();
                    }
                }
            }
        });
    }

    //start and bind the service when the activity starts
    @Override
    protected void onStart() {
        super.onStart();

    }

    private void bindAndStartSrv() {
        Log.d(LOG_ERR_TAG, "bindAndStartSrv() begin ");
        playIntent = new Intent(this, MediaPlayerService.class);
        if(!leftWhilePlaying)
            startService(playIntent);
        bindService(playIntent, musicConnection, 0); // Context.BIND_AUTO_CREATE
        Log.d(LOG_ERR_TAG, "bindAndStartSrv() end ");
    }

    private void initToolbar() {
        Toolbar displayActToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(displayActToolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:

                return true;

            case R.id.settings:

                //Intent fullScrAuthorInfoIntent = new Intent(this, FullscreenActivityAuthorDisplay.class);
                //this.startActivity(fullScrAuthorInfoIntent);

                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (MediaPlayerService.MusicBinder)service;
            //get service
            mService = binder.getService();
            //pass list
            //mService.setList(songList);
            musicBound = true;
            binder.registerActivity(MainActivity.this, listener);

            if (mService.isPlaying()){
                mSeekBar.setMax(mService.getPlayerDuration()/1000);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
            binder = null;
        }
    };

    @Override
    public void onStop(){
        super.onStop();
        saveSongsArrayState();
        if(mService != null && mService.isPlaying()){
            playButtonUnusualState = true;
            savePlayPauseState();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mService != null && !mService.isPlaying()) {
            MediaPlayerService.stop(MainActivity.this);
        }
        else {
            playButtonUnusualState = true;
            savePlayPauseState();
        }
        if (binder != null)
            binder.unregisterActivity(this);
        if (mService != null)
            unbindService(musicConnection);
        if (requestHandler != null) {
            requestHandler.removeCallbacks(mRunnable);
        }
    }

    private void savePlayPauseState(){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = sharedPref.edit();
        prefsEditor.putBoolean(KEY_LEFT_WHILE_PLAYING, playButtonUnusualState);
        prefsEditor.apply();
    }

    private void saveSongsArrayState() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = sharedPref.edit();
        Gson gson = new Gson();
        String jsonText = gson.toJson(songsPaths);
        prefsEditor.putString(KEY_SONGS_PATHS, jsonText);
        prefsEditor.apply();
    }
/*
    private void saveServiceState(){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = sharedPref.edit();

        Gson gson = new Gson();
        String json = gson.toJson(mService);
        prefsEditor.putString(KEY_SERVICE, json);
        String json2 = gson.toJson(playIntent);
        prefsEditor.putString(KEY_PLAY_INTENT, json2);
        prefsEditor.apply();
    }
*/

    private void restoreState(){

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        if (!permissionToReadExtStorageAccepted)
            return;

        // Restore songs array
        if(sharedPref.contains(KEY_SONGS_PATHS)) {
            Gson gson = new Gson();
            String jsonText = sharedPref.getString(KEY_SONGS_PATHS, null);
            songsPaths = gson.fromJson(jsonText, ArrayList.class);
        }
        else
            processMusicFiles();

        SharedPreferences.Editor preferencesEditor = sharedPref.edit();
        preferencesEditor.clear();
        preferencesEditor.apply();
    }

    private void restorePlayPauseButtonState() {
        // Restore play / pause button state
        if(leftWhilePlaying) {
            playPauseButton.setChecked(!playPauseButton.isChecked());
        }
    }

    // Callback for service to use to notify activity about something
    private IListenerFunctions listener = new IListenerFunctions() {
        public void setSeekBarMaxDuration(int maxDuration) {    // in ms
            if (mSeekBar != null)
                mSeekBar.setMax(maxDuration/1000);
        }
        public void onMediaPlayerCompletion(){
            playButtonUnusualState = true;
            playPauseButton.setChecked(!playPauseButton.isChecked());
        }
    };
}

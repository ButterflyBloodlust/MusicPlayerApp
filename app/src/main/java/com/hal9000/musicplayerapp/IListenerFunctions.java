package com.hal9000.musicplayerapp;

// Service clients implements this to get callbacks from service.

public interface IListenerFunctions {
    public void setSeekBarMaxDuration(int maxDuration);
    public void onMediaPlayerCompletion();
}

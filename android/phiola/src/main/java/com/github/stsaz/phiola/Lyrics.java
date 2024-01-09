package com.github.stsaz.phiola;

import android.util.Log;

public class Lyrics extends Filter {
    public Lyrics(Track track) {
    }
    public int open(TrackHandle t) {
        Log.d("me", "open: "+t.url);
        return 0;
    }
    public int process(TrackHandle t) {
        return 0;
    }
}

package com.github.stsaz.phiola;

import android.util.Log;

import com.github.stsaz.phiola.databinding.MainBinding;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import neoe.util.FileUtil;
import neoe.util.Finder;

public class Lyrics extends Filter {
    private static final String TAG = "Lyrics";
    private static final int QSIZE = 5;
    private final MainActivity acti;
    private final Track track;
    private String url;
    private Integer pos;
    private boolean justOpened;

    public Lyrics(Track track, MainActivity acti) {
        this.track = track;
        this.acti = acti;
    }

    public static class Line {
        String text;
        long ts;
    }

    List<Line> lines;
    int lastPos;
    static Map<String, List<Line>> cache = Collections.synchronizedMap(new HashMap<>());

    public int open(TrackHandle t) {
        String url = t.url;
        this.url = url;
        lines = null;
        lastPos = -1;
        Log.d("me", "open: " + url);
        lines = cache.get(url);
        if (lines != null) {
            justOpened = true;
            return 0;
        }
        int p1 = url.lastIndexOf(".");
        if (p1 < 0) return 0;
        String url2 = url.substring(0, p1) + ".lrc";
        try {
            lines = readLyrics(url2);
            cache.put(url, lines);
            Log.d(TAG, String.format("open:read %,d lines of lyrics", lines.size()));
            if (lines.isEmpty()) lines = null;
            else {
                setLinePos(0);
                justOpened = true;
            }
        } catch (Throwable ex) {
            Log.e("lyrics", "open: ", ex);
        }
        return 0;
    }

    private boolean resume() {
        if (lines == null) return false;
        try {
            Integer pos = SmallPersistantMap.getInt(url);
            Log.d(TAG, String.format("try resume:(%s) for %s", pos, url));
            if (pos == null) return false;
            int ms = (int) lines.get(pos).ts;
            ms -= 1000;//why, but needed...
            if (ms < 0) ms = 0;
            track.seek(ms);
            Log.d(TAG, "resume: linepos=" + pos);
            return true;
        } catch (Exception ex) {
            Log.w("lyrics", "resume: " + ex);
        }
        return false;
    }

    private void setLinePos(int p) {
        if (lines == null) return;
        if (p < 0) p = 0;
        p = Math.min(p, lines.size() - 1);
        if (p != lastPos) {
            if (setUILines(p)) lastPos = p;
        }
    }

    private boolean setUILines(int p) {
        try {
            MainBinding b = acti.b;
            if (b != null) {// init timing
                acti.runOnUiThread(() -> {
                    try {
                        b.lineCurrent.setText(getLineText(p));
                        b.lineN1.setText(getLineText(p + 1));
                        b.lineP1.setText(getLineText(p - 1));
                        b.lineP2.setText(getLineText(p - 2));
                    } catch (Exception ex) {
                        Log.w(TAG, "setUILines: " + ex);
                    }
                });
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            Log.w(TAG, "setUILines: " + ex);
        }
        return false;
    }

    private String getLineText(int p) {
        if (p < 0 || p >= lines.size()) return "";
        return specialFilter1(lines.get(p).text);
    }

    private static String specialFilter1(String s) {
        if (s == null) return "";
        //step1
        {
            int p1 = s.indexOf("--(");
            if (p1 > 0) {
                s = s.substring(p1 + 3);
                if (s.endsWith(")")) s = s.substring(0, s.length() - 1);
            }
        }
        //step2
        int safe = 0;
        while (true) {
            if (safe++ > 10) break;
            int p1 = s.indexOf("(");
            if (p1 < 0) break;
            int p2 = s.indexOf(")", p1 + 1);
            if (p2 < 0) break;
            String s2 = s.substring(p1 + 1, p2);
            if (s2.startsWith("st ") || allIn(s2, "0123456789,/")) {
                s = s.substring(0, p1) + s.substring(p2 + 1);
            } else break;
        }
        return s;
    }

    private static boolean allIn(String s, String pat) {
        for (char c : s.toCharArray()) {
            if (pat.indexOf(c) < 0) return false;
        }
        return true;
    }

    private List<Line> readLyrics(String url) throws IOException {
        String text0 = FileUtil.readString(new FileInputStream(url), null);
        List<Line> res = new ArrayList<>();
        Finder f = new Finder(text0);
        f.find("[");
        while (true) {
            if (f.finished()) break;
            String ts = f.readUntil("]");
            long ts1 = parseLrcTime(ts);
            if (ts1 < 0) {//failed
                f.find("[");
                continue;
            }
            String line0 = f.readUntil("\n") + f.readUntil("[").trim();
            Line line = new Line();
            line.ts = ts1;
            line.text = line0;
            res.add(line);
        }
        //no sort by ts, assume source data is sorted
        return res;
    }

    private long parseLrcTime(String ts) {
        int p1 = ts.indexOf(":");
        if (p1 <= 0) return -1;
        try {
            return Long.parseLong(ts.substring(0, p1).trim()) * 60000 + (long) (1000 * Float.parseFloat(ts.substring(p1 + 1).trim()));
        } catch (Exception ex) {
        }
        return -1;
    }

    //    public void close(TrackHandle t) {
//    }
//    public void closed(TrackHandle t) {
//    }

    static class FixSizedQueue<T> {
        int maxsize;

        FixSizedQueue(int size) {
            this.maxsize = size;
        }

        void add(T v) {
            queue.add(v);
            if (queue.size() > maxsize) queue.removeFirst();
        }

        int size() {
            return queue.size();
        }

        LinkedList<T> queue = new LinkedList<T>();
    }

    FixSizedQueue queue = new FixSizedQueue<Integer>(QSIZE);

    public int process(TrackHandle tr) {
        if (lines == null) return 0;
        if (justOpened && track.state() == Track.STATE_PLAYING) {
            justOpened = false;
            if (resume()) return 0;
        }
        int pos = tr.pos_msec;
        int size = lines.size();
        int f = size - 1;
        for (int i = 0; i < size; i++) {
            if (lines.get(i).ts > pos) {
                f = i - 1;
                break;
            }
        }
        if (f < 0) f = 0;
        Log.d(TAG, String.format("process: pos=%,d and linepos=%,d", pos, f));
        setLinePos(f);
        queue.add(f);
        if (f > 0 && queue.size() >= QSIZE
                && ((Integer) queue.queue.getLast()) - ((Integer) queue.queue.getFirst())
                <= QSIZE * 2) {
            try {
                SmallPersistantMap.put(url, f);
                Log.d(TAG, "record linepos=" + f);
            } catch (Exception ex) {
                Log.e(TAG, "SmallPersistantMap.save " + ex);
            }
            queue.queue.clear();// wait for another round
        }
        return 0;
    }


}

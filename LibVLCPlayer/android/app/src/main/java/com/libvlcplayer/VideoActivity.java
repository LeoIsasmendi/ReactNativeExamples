package com.libvlcplayer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;

public class VideoActivity extends Activity implements IVLCVout.Callback {
    public final static String TAG = "VideoActivity";

    public static final String RTSP_URL = "rtspurl";
    public static final String RTSP_USR = "rtspusr";
    public static final String RTSP_PWD = "rtsppwd";

    String mRtspUrl, mRtspUsr, mRtspPwd;

    // media player
    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;


    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = false;

    private VLCVideoLayout mVideoLayout = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate in VideoActivity");


        setContentView(R.layout.videoactivity);

        // Get URL
        Intent intent = getIntent();
        mRtspUrl = intent.getExtras().getString(RTSP_URL);
        mRtspUsr = intent.getExtras().getString(RTSP_USR);
        mRtspPwd = intent.getExtras().getString(RTSP_PWD);

        Log.d(TAG, "Playing back " + mRtspUrl);

        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        args.add(String.format("--rtsp-user=%s", mRtspUsr));
        args.add(String.format("--rtsp-pwd=%s", mRtspPwd));
        args.add("--aout=opensles");
        args.add("--audio-time-stretch"); // time stretching
        args.add("-vvv"); // verbosity
        args.add("--avcodec-codec=h264");
        args.add("--file-logging");
        args.add("--rtsp-tcp");
        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);

        mVideoLayout = findViewById(R.id.video_layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume in VideoActivity");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause in VideoActivity");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMediaPlayer.attachViews(mVideoLayout, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);

        try {
            final Media media = new Media(mLibVLC, Uri.parse(mRtspUrl));
            mMediaPlayer.setMedia(media);
            media.release();
        } catch (Exception e) {
            throw new RuntimeException("Invalid asset folder");
        }
        mMediaPlayer.play();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy in VideoActivity");
        mMediaPlayer.release();
        mLibVLC.release();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMediaPlayer.stop();
        mMediaPlayer.detachViews();
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {
        Log.d(TAG, "onSurfacesCreated in VideoActivity");
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {
        Log.d(TAG, "onSurfacesDestroyed in VideoActivity");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        Log.d(TAG, "onPointerCaptureChanged in VideoActivity");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Log.d(TAG, "Back key pressed in VideoActivity");
            setResult(RESULT_OK);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void releasePlayer() {
        Log.d(TAG, "releasePlayer in VideoActivity");
    }

}
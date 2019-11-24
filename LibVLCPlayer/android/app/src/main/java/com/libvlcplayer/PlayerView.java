package com.libvlcplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.annotations.ReactProp;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
class PlayerView extends TextureView implements LifecycleEventListener, TextureView.SurfaceTextureListener,
        AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "PlayerView";

    private final VideoEventEmitter eventEmitter;
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private TextureView surfaceView;
    private Surface surfaceVideo;
    private boolean isSurfaceViewDestory;
    private int surfaceW, surfaceH;

    private String src;

    private ReadableMap srcMap;

    private int mVideoHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoVisibleHeight = 0;
    private int mVideoVisibleWidth = 0;
    private int mSarNum = 0;
    private int mSarDen = 0;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private boolean isPaused = true;
    private boolean isHostPaused = false;
    private int preVolume = 200;
    private boolean autoAspectRatio = false;

    // React
    private final ThemedReactContext themedReactContext;
    private final ReactApplicationContext applicationContext;
    private final AudioManager audioManager;


    public PlayerView(ThemedReactContext context, ReactApplicationContext applicationContext) {
        super(context);
        this.applicationContext = applicationContext;
        this.eventEmitter = new VideoEventEmitter(context);
        this.themedReactContext = context;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // themedReactContext.addLifecycleEventListener(this);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenHeight = dm.heightPixels;
        screenWidth = dm.widthPixels;
        this.setSurfaceTextureListener(this);
        this.addOnLayoutChangeListener(onLayoutChangeListener);

    }

    @Override
    public void setId(int id) {
        super.setId(id);
        eventEmitter.setViewId(id);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // createPlayer();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopPlayback();
    }

    // LifecycleEventListener implementation

    @Override
    public void onHostResume() {
        if (mMediaPlayer != null && isSurfaceViewDestory && isHostPaused) {
            IVLCVout vlcOut = mMediaPlayer.getVLCVout();
            if (!vlcOut.areViewsAttached()) {
                // vlcOut.setVideoSurface(this.getHolder().getSurface(), this.getHolder());
                vlcOut.attachViews(onNewVideoLayoutListener);
                isSurfaceViewDestory = false;
                isPaused = false;
                // this.getHolder().setKeepScreenOn(true);
                mMediaPlayer.play();
            }
        }
    }

    @Override
    public void onHostPause() {
        if (!isPaused && mMediaPlayer != null) {
            isPaused = true;
            isHostPaused = true;
            mMediaPlayer.pause();
            // this.getHolder().setKeepScreenOn(false);
            WritableMap map = Arguments.createMap();
            map.putString("type", "Paused");
            eventEmitter.onVideoStateChange(map);
        }
        Log.i("onHostPause", "---------onHostPause------------>");
    }

    @Override
    public void onHostDestroy() {
        stopPlayback();
    }

    // AudioManager.OnAudioFocusChangeListener implementation
    @Override
    public void onAudioFocusChange(int focusChange) {
    }

    /*************
     * Events Listener
     *************/

    private View.OnLayoutChangeListener onLayoutChangeListener = new View.OnLayoutChangeListener() {

        @Override
        public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
            if (view.getWidth() > 0 && view.getHeight() > 0) {
                mVideoWidth = view.getWidth();
                mVideoHeight = view.getHeight();
                if (mMediaPlayer != null) {
                    IVLCVout vlcOut = mMediaPlayer.getVLCVout();
                    vlcOut.setWindowSize(mVideoWidth, mVideoHeight);
                    if (autoAspectRatio) {
                        mMediaPlayer.setAspectRatio(mVideoWidth + ":" + mVideoHeight);
                    }
                }
            }
        }
    };

    private MediaPlayer.EventListener mPlayerListener = new MediaPlayer.EventListener() {
        long currentTime = 0;
        long totalLength = 0;

        @Override
        public void onEvent(MediaPlayer.Event event) {
            boolean isPlaying = mMediaPlayer.isPlaying();
            currentTime = mMediaPlayer.getTime();
            float position = mMediaPlayer.getPosition();
            totalLength = mMediaPlayer.getLength();

            WritableMap map = Arguments.createMap();
            map.putBoolean("isPlaying", isPlaying);
            map.putDouble("position", position);
            map.putDouble("currentTime", currentTime);
            map.putDouble("duration", totalLength);

            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    map.putString("type", "Ended");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.Playing:
                    map.putString("type", "Playing");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.Opening:
                    map.putString("type", "Opening");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.Paused:
                    map.putString("type", "Paused");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.Buffering:
                    map.putDouble("bufferRate", event.getBuffering());
                    map.putString("type", "Buffering");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.Stopped:
                    map.putString("type", "Stopped");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.EncounteredError:
                    map.putString("type", "Error");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.TimeChanged:
                    map.putString("type", "TimeChanged");
                    eventEmitter.onVideoStateChange(map);
                    break;
                default:
                    map.putString("type", event.type + "");
                    eventEmitter.onVideoStateChange(map);
                    break;
            }
            eventEmitter.isPlaying(mMediaPlayer.isPlaying());
        }
    };

    private IVLCVout.OnNewVideoLayoutListener onNewVideoLayoutListener = new IVLCVout.OnNewVideoLayoutListener() {
        @Override
        public void onNewVideoLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight,
                                     int sarNum, int sarDen) {
            if (width * height == 0)
                return;
            // store video size
            mVideoWidth = width;
            mVideoHeight = height;
            mVideoVisibleWidth = visibleWidth;
            mVideoVisibleHeight = visibleHeight;
            mSarNum = sarNum;
            mSarDen = sarDen;
            WritableMap map = Arguments.createMap();
            map.putInt("mVideoWidth", mVideoWidth);
            map.putInt("mVideoHeight", mVideoHeight);
            map.putInt("mVideoVisibleWidth", mVideoVisibleWidth);
            map.putInt("mVideoVisibleHeight", mVideoVisibleHeight);
            map.putInt("mSarNum", mSarNum);
            map.putInt("mSarDen", mSarDen);
            map.putString("type", "onNewVideoLayout");
            eventEmitter.onVideoStateChange(map);
        }
    };

    IVLCVout.Callback callback = new IVLCVout.Callback() {
        @Override
        public void onSurfacesCreated(IVLCVout ivlcVout) {
            isSurfaceViewDestory = false;
        }

        @Override
        public void onSurfacesDestroyed(IVLCVout ivlcVout) {
            isSurfaceViewDestory = true;
        }

    };

    /*************
     * MediaPlayer
     *************/

    private void stopPlayback() {
        onStopPlayback();
        releasePlayer();
    }

    private void onStopPlayback() {
        setKeepScreenOn(false);
        audioManager.abandonAudioFocus(this);
    }

    private void createPlayer(boolean autoplayResume, boolean isResume) {
        releasePlayer();
        if (this.getSurfaceTexture() == null) {
            return;
        }

        try {

            String uriString = srcMap.hasKey("uri") ? srcMap.getString("uri") : null;
            initMedia(uriString);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initMedia(String uriString) {
        // Create LibVLC
        libvlc = new LibVLC(getContext(), null);

        // Create media player
        mMediaPlayer = new MediaPlayer(libvlc);
        mMediaPlayer.setEventListener(mPlayerListener);
        IVLCVout vlcOut = mMediaPlayer.getVLCVout();

        if (mVideoWidth > 0 && mVideoHeight > 0) {
            vlcOut.setWindowSize(mVideoWidth, mVideoHeight);
            if (autoAspectRatio) {
                mMediaPlayer.setAspectRatio(mVideoWidth + ":" + mVideoHeight);
            }
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        Media m = null;

        Uri uri = Uri.parse(uriString);
        m = new Media(libvlc, uri);
        m.setEventListener(mMediaListener);

        mMediaPlayer.setMedia(m);
        mMediaPlayer.setScale(0);

        if (!vlcOut.areViewsAttached()) {
            vlcOut.addCallback(callback);
            vlcOut.setVideoSurface(this.getSurfaceTexture());
            vlcOut.attachViews(onNewVideoLayoutListener);
        }
        mMediaPlayer.play();
        eventEmitter.loadStart();
    }

    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(callback);
        vout.detachViews();
        libvlc.release();
        libvlc = null;
    }

    /**
     * 视频进度调整
     *
     * @param time
     */
    public void seekTo(long time) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setTime(time);
        }
    }

    public void setPosition(float position) {
        if (mMediaPlayer != null) {
            if (position >= 0 && position <= 1) {
                mMediaPlayer.setPosition(position);
            }
        }
    }

    /**
     * 设置资源路径
     *
     * @param uri
     * @param isLive
     */
    public void setSrc(String uri, boolean isLive, boolean autoplay) {
        this.src = uri;
        createPlayer(autoplay, false);
    }

    public void setSrc(ReadableMap src) {
        this.srcMap = src;
        createPlayer(true, false);
    }

    /**
     * 改变播放速率
     *
     * @param rateModifier
     */
    public void setRateModifier(float rateModifier) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setRate(rateModifier);
        }
    }

    /**
     * 改变声音大小
     *
     * @param volumeModifier
     */
    public void setVolumeModifier(int volumeModifier) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(volumeModifier);
        }
    }

    /**
     * 改变静音状态
     *
     * @param muted
     */
    public void setMutedModifier(boolean muted) {
        if (mMediaPlayer != null) {
            if (muted) {
                this.preVolume = mMediaPlayer.getVolume();
                mMediaPlayer.setVolume(0);
            } else {
                mMediaPlayer.setVolume(this.preVolume);
            }
        }
    }

    /**
     * 改变播放状态
     *
     * @param paused
     */
    public void setPausedModifier(boolean paused) {
        Log.i("paused:", "" + paused + ":" + mMediaPlayer);
        if (mMediaPlayer != null) {
            if (paused) {
                isPaused = true;
                mMediaPlayer.pause();
            } else {
                isPaused = false;
                mMediaPlayer.play();
                Log.i("do play:", true + "");
            }
        } else {
            createPlayer(!paused, false);
        }
    }

    /**
     * 截图
     *
     * @param path
     */
    public void doSnapshot(String path) {
        if (mMediaPlayer != null) {
            // int result = new RecordEvent().takeSnapshot(mMediaPlayer, path, 0, 0);
            // TODO fix this, RecordEvent its a deprecated Method in the new version of
            // LIBVLC
            int result = 0;
            if (result == 0) {
                eventEmitter.onSnapshot(1);
            } else {
                eventEmitter.onSnapshot(0);
            }
        }

    }

    /**
     * 重新加载视频
     *
     * @param autoplay
     */
    public void doResume(boolean autoplay) {
        createPlayer(autoplay, true);
    }

    public void setRepeatModifier(boolean repeat) {
    }

    /**
     * 改变宽高比
     *
     * @param aspectRatio
     */
    public void setAspectRatio(String aspectRatio) {
        if (!autoAspectRatio && mMediaPlayer != null) {
            mMediaPlayer.setAspectRatio(aspectRatio);
        }
    }

    public void setAutoAspectRatio(boolean auto) {
        autoAspectRatio = auto;
    }

    public void cleanUpResources() {
        if (surfaceView != null) {
            surfaceView.removeOnLayoutChangeListener(onLayoutChangeListener);
        }
        stopPlayback();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        surfaceVideo = new Surface(surface);
        createPlayer(true, false);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.i("onSurfaceTextureUpdated", "onSurfaceTextureUpdated");
    }

    private final Media.EventListener mMediaListener = new Media.EventListener() {
        @Override
        public void onEvent(Media.Event event) {
            switch (event.type) {
                case Media.Event.MetaChanged:
                    Log.i(TAG, "Media.Event.MetaChanged:  =" + event.getMetaId());
                    break;
                case Media.Event.ParsedChanged:
                    Log.i(TAG, "Media.Event.ParsedChanged  =" + event.getMetaId());
                    break;
                case Media.Event.StateChanged:
                    Log.i(TAG, "StateChanged   =" + event.getMetaId());
                    break;
                default:
                    Log.i(TAG, "Media.Event.type=" + event.type + "   eventgetParsedStatus=" + event.getParsedStatus());
                    break;

            }
        }
    };
}


package com.libvlcplayer;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import android.util.Log;

import java.util.Map;

public class PlayerViewManager extends SimpleViewManager<PlayerView> {

    private static final String REACT_CLASS = "VLCPlayer";

    private static final String PROP_SRC = "source";
    private static final String PROP_SRC_URI = "uri";
    private static final String PROP_SRC_TYPE = "type";
    private static final String PROP_REPEAT = "repeat";
    private static final String PROP_PAUSED = "paused";
    private static final String PROP_MUTED = "muted";
    private static final String PROP_VOLUME = "volume";
    private static final String PROP_SEEK = "seek";
    private static final String PROP_RESUME = "resume";
    private static final String PROP_RATE = "rate";
    private static final String PROP_POSITION = "position";
    private static final String PROP_VIDEO_ASPECT_RATIO = "videoAspectRatio";
    private static final String PROP_SRC_IS_NETWORK = "isNetwork";
    private static final String PROP_SNAPSHOT_PATH = "snapshotPath";
    private static final String PROP_AUTO_ASPECT_RATIO = "autoAspectRatio";
    private static final String PROP_CLEAR = "clear";

    private ReactApplicationContext mReactApplicationContext;

    @Override
    public String getName() {
        return REACT_CLASS;
    }


    public PlayerViewManager(ReactApplicationContext reactApplicationContext) {
        mReactApplicationContext = reactApplicationContext;
    }

    @Override
    protected PlayerView createViewInstance(ThemedReactContext themedReactContext) {
        return new PlayerView(themedReactContext, mReactApplicationContext);
    }

    @Override
    public void onDropViewInstance(PlayerView view) {
        view.cleanUpResources();
    }

    @Override
    public @Nullable
    Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
        for (String event : VideoEventEmitter.Events) {
            builder.put(event, MapBuilder.of("registrationName", event));
        }
        return builder.build();
    }

    @ReactProp(name = PROP_CLEAR)
    public void setClear(final PlayerView videoView, final boolean clear) {
        videoView.cleanUpResources();
    }


    @ReactProp(name = PROP_SRC)
    public void setSrc(final PlayerView videoView, @Nullable ReadableMap src) {
        Context context = videoView.getContext().getApplicationContext();

        String uriString = src.hasKey(PROP_SRC_URI) ? src.getString(PROP_SRC_URI) : null;
        if (TextUtils.isEmpty(uriString)) {
            return;
        }
        videoView.setSrc(src);
    }

    @ReactProp(name = PROP_REPEAT, defaultBoolean = false)
    public void setRepeat(final PlayerView videoView, final boolean repeat) {
        videoView.setRepeatModifier(repeat);
    }

    @ReactProp(name = PROP_PAUSED, defaultBoolean = false)
    public void setPaused(final PlayerView videoView, final boolean paused) {
        videoView.setPausedModifier(paused);
    }

    @ReactProp(name = PROP_MUTED, defaultBoolean = false)
    public void setMuted(final PlayerView videoView, final boolean muted) {
        videoView.setMutedModifier(muted);
    }

    @ReactProp(name = PROP_VOLUME, defaultFloat = 1.0f)
    public void setVolume(final PlayerView videoView, final float volume) {
        videoView.setVolumeModifier((int)volume);
    }

    @ReactProp(name = PROP_SEEK)
    public void setSeek(final PlayerView videoView, final float seek) {
        videoView.seekTo(Math.round(seek * 1000f));
        //videoView.seekTo(seek);
    }

    @ReactProp(name = PROP_AUTO_ASPECT_RATIO, defaultBoolean = false)
    public void setAutoAspectRatio(final PlayerView videoView, final boolean autoPlay) {
        videoView.setAutoAspectRatio(autoPlay);
    }

    @ReactProp(name = PROP_RESUME, defaultBoolean = true)
    public void setResume(final PlayerView videoView, final boolean autoPlay) {
        videoView.doResume(autoPlay);
    }

    @ReactProp(name = PROP_RATE)
    public void setRate(final PlayerView videoView, final float rate) {
        videoView.setRateModifier(rate);
    }

    @ReactProp(name = PROP_POSITION)
    public void setPosition(final PlayerView videoView, final float potision) {
        videoView.setPosition(potision);
    }

    @ReactProp(name = PROP_SNAPSHOT_PATH)
    public void setSnapshotPath(final PlayerView videoView, final String snapshotPath) {
        videoView.doSnapshot(snapshotPath);
    }

}

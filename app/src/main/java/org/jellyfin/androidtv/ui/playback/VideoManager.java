package org.jellyfin.androidtv.ui.playback;

import android.app.Activity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.channelflow.ChannelFlowChannel;
import org.jellyfin.androidtv.channelflow.ChannelFlowClientLogs;
import org.jellyfin.androidtv.channelflow.ChannelFlowConnectionStore;
import org.jellyfin.androidtv.channelflow.ChannelFlowGuideRepository;
import org.jellyfin.androidtv.channelflow.ChannelFlowStream;
import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.preference.constant.ZoomMode;
import org.jellyfin.sdk.api.client.ApiClient;
import org.jellyfin.sdk.model.api.MediaStream;
import org.jellyfin.sdk.model.api.MediaStreamType;
import org.koin.java.KoinJavaComponent;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.List;

import timber.log.Timber;

public class VideoManager {
    private ZoomMode mZoomMode = ZoomMode.FIT;
    private Activity mActivity;
    private PlaybackOverlayFragmentHelper _helper;
    private VlcVideoEngine vlcEngine;
    private View videoSurface;

    private long mMetaDuration = -1;

    public boolean isContracted = false;

    int normalWidth;
    int normalHeight;

    public VideoManager(@NonNull Activity activity, @NonNull View view, @NonNull PlaybackOverlayFragmentHelper helper) {
        mActivity = activity;
        _helper = helper;

        VLCVideoLayout vlcLayout = view.findViewById(R.id.vlc_video_layout);
        if (vlcLayout == null) {
            Timber.e("VLC video layout is missing");
            helper.getFragment().closePlayer();
            return;
        }

        vlcLayout.setVisibility(View.VISIBLE);
        vlcEngine = new VlcVideoEngine(activity, vlcLayout, helper);
        videoSurface = vlcLayout;
    }

    public void subscribe(@NonNull PlaybackControllerNotifiable notifier) {
        if (vlcEngine != null) vlcEngine.subscribe(notifier);
    }

    public boolean isUsingVlc() {
        return vlcEngine != null;
    }

    public boolean isInitialized() {
        return vlcEngine != null;
    }

    public @NonNull ZoomMode getZoomMode() {
        return mZoomMode;
    }

    public void setZoom(@NonNull ZoomMode mode) {
        mZoomMode = mode;
        if (vlcEngine != null) vlcEngine.setZoom(mode);
    }

    public void setMetaDuration(long duration) {
        mMetaDuration = duration;
    }

    public long getDuration() {
        if (vlcEngine == null) return mMetaDuration;
        long duration = vlcEngine.getDuration();
        return duration > 0 ? duration : mMetaDuration;
    }

    public long getBufferedPosition() {
        return -1;
    }

    public long getCurrentPosition() {
        return vlcEngine != null ? vlcEngine.getCurrentPosition() : 0;
    }

    public boolean isPlaying() {
        return vlcEngine != null && vlcEngine.isPlaying();
    }

    public void start() {
        if (vlcEngine == null) {
            Timber.e("VLC engine should not be null");
            _helper.getFragment().closePlayer();
            return;
        }
        vlcEngine.start();
        normalWidth = videoSurface.getLayoutParams().width;
        normalHeight = videoSurface.getLayoutParams().height;
    }

    public void play() {
        if (vlcEngine != null) vlcEngine.play();
    }

    public void pause() {
        if (vlcEngine != null) vlcEngine.pause();
    }

    public void stopPlayback() {
        if (vlcEngine != null) vlcEngine.stop();
    }

    public boolean isSeekable() {
        return vlcEngine != null && vlcEngine.isSeekable();
    }

    public long seekTo(long pos) {
        if (vlcEngine == null) return -1;
        return vlcEngine.seekTo(pos);
    }

    public void setMediaStreamInfo(ApiClient api, StreamInfo streamInfo) {
        String path = streamInfo.getMediaUrl();
        if (path == null) {
            Timber.w("Video path is null cannot continue");
            return;
        }
        if (vlcEngine == null) {
            Timber.e("VLC engine should not be null");
            return;
        }

        Timber.i("Video path set to: %s", ChannelFlowClientLogs.redactSecrets(path));

        boolean liveStream = ChannelFlowStream.INSTANCE.isLive(path);
        ChannelFlowChannel channel = null;
        if (streamInfo.getItemId() != null) {
            ChannelFlowGuideRepository catalog = KoinJavaComponent.get(ChannelFlowGuideRepository.class);
            channel = catalog.findChannel(streamInfo.getItemId());
        }
        ChannelFlowConnectionStore store = KoinJavaComponent.get(ChannelFlowConnectionStore.class);
        String apiKey = store.getConnection() != null ? store.getConnection().getApiKey() : null;
        vlcEngine.setMedia(
                path,
                liveStream,
                channel != null ? channel.getName() : null,
                channel != null ? channel.getId() : streamInfo.getItemId(),
                channel != null ? channel.getNumber() : null,
                channel != null ? channel.getLogoUrl() : null,
                apiKey
        );
    }

    public int getExoPlayerTrack(@Nullable MediaStreamType streamType, @Nullable List<MediaStream> allStreams) {
        return -1;
    }

    public boolean setExoPlayerTrack(int index, @Nullable MediaStreamType streamType, @Nullable List<MediaStream> allStreams) {
        return false;
    }

    public float getPlaybackSpeed() {
        return vlcEngine != null ? vlcEngine.getPlaybackSpeed() : 1.0f;
    }

    public void setPlaybackSpeed(float speed) {
        if (speed < 0.25) {
            Timber.w("Invalid playback speed requested: %f", speed);
            return;
        }
        Timber.d("Setting playback speed: %f", speed);
        if (vlcEngine != null) vlcEngine.setPlaybackSpeed(speed);
    }

    public void destroy() {
        stopPlayback();
        releasePlayer();
    }

    private void releasePlayer() {
        _helper.setScreensaverLock(false);
        if (vlcEngine != null) {
            vlcEngine.release();
            vlcEngine = null;
        }
    }

    public void contractVideo(int height) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) videoSurface.getLayoutParams();
        if (isContracted) return;

        int sw = mActivity.getWindow().getDecorView().getWidth();
        int sh = mActivity.getWindow().getDecorView().getHeight();
        float ar = (float) sw / sh;
        lp.height = height;
        lp.width = (int) Math.ceil(height * ar);
        lp.rightMargin = ((lp.width - normalWidth) / 2) - 110;
        lp.bottomMargin = ((lp.height - normalHeight) / 2) - 50;

        videoSurface.setLayoutParams(lp);
        videoSurface.invalidate();

        isContracted = true;
    }

    public void setVideoFullSize(boolean force) {
        if (normalHeight == 0) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) videoSurface.getLayoutParams();
        if (force) {
            lp.height = -1;
            lp.width = -1;
        } else {
            lp.height = normalHeight;
            lp.width = normalWidth;
        }

        lp.rightMargin = 0;
        lp.bottomMargin = 0;
        videoSurface.setLayoutParams(lp);
        videoSurface.invalidate();

        isContracted = false;
    }
}

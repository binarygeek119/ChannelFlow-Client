package org.jellyfin.androidtv.ui.itemhandling;

import android.content.Context;

import org.jellyfin.androidtv.ui.playback.PlaybackLauncher;
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter;
import org.jellyfin.androidtv.util.PlaybackHelper;
import org.jellyfin.androidtv.util.apiclient.Response;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

public class ItemLauncher {
    private final Lazy<PlaybackLauncher> playbackLauncher = KoinJavaComponent.<PlaybackLauncher>inject(PlaybackLauncher.class);
    private final Lazy<PlaybackHelper> playbackHelper = KoinJavaComponent.<PlaybackHelper>inject(PlaybackHelper.class);

    public void launch(final BaseRowItem rowItem, MutableObjectAdapter<Object> adapter, final Context context) {
        switch (rowItem.getBaseRowType()) {
            case LiveTvProgram:
                BaseItemDto program = rowItem.getBaseItem();
                if (program == null || program.getChannelId() == null) return;
                ItemLauncherHelper.getItem(program.getChannelId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        if (!isActive()) return;
                        List<BaseItemDto> items = new ArrayList<>(1);
                        items.add(response);
                        playbackLauncher.getValue().launch(context, items);
                    }
                });
                break;

            case LiveTvChannel:
                final BaseItemDto channel = rowItem.getBaseItem();
                ItemLauncherHelper.getItem(channel.getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        if (!isActive()) return;
                        playbackHelper.getValue().getItemsToPlay(context, response, false, false, new Response<List<BaseItemDto>>() {
                            @Override
                            public void onResponse(List<BaseItemDto> response) {
                                if (!isActive()) return;
                                playbackLauncher.getValue().launch(context, response);
                            }
                        });
                    }
                });
                break;

            default:
                Timber.d("Ignoring non-live-TV item launch: %s", rowItem.getBaseRowType());
                break;
        }
    }
}

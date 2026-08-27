package org.jellyfin.androidtv.ui;

import static org.koin.java.KoinJavaComponent.get;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.preference.LiveTvPreferences;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuide;
import org.jellyfin.androidtv.util.DateTimeExtensionsKt;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.sdk.BaseItemExtensionsKt;
import org.jellyfin.sdk.model.api.BaseItemDto;

import java.time.LocalDateTime;

public class ProgramGridCell extends RelativeLayout {

    private LiveTvGuide mActivity;
    private TextView mProgramName;
    private LinearLayout mInfoRow;
    private BaseItemDto mProgram;
    private ImageView mRecIndicator;
    private int mBackgroundColor = 0;
    private boolean isLast;
    private boolean isFirst;

    public ProgramGridCell(Context context, LiveTvGuide activity, BaseItemDto program, boolean keyListen) {
        super(context);
        initComponent((Activity) context, activity, program, keyListen);
    }

    private void initComponent(Activity context, LiveTvGuide activity, BaseItemDto program, boolean keyListen) {
        mActivity = activity;

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.program_grid_cell, this, false);
        this.addView(v);

        setFocusable(true);

        mProgramName = findViewById(R.id.programName);
        mInfoRow = findViewById(R.id.infoRow);
        mProgramName.setText(program.getName());
        mProgramName.setTextColor(ContextCompat.getColor(context, R.color.channelflow_text));
        mProgram = program;
        mRecIndicator = findViewById(R.id.recIndicator);

        setCellBackground();
        applyGuideChrome(false);

        if (program.getStartDate() != null && program.getEndDate() != null) {
            LocalDateTime localStart = program.getStartDate();
            if (localStart.plusMinutes(1).isBefore(activity.getCurrentLocalStartDate())) {
                mProgramName.setText("<< "+mProgramName.getText());
                TextView time = new TextView(context);
                time.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                time.setTextSize(12);
                time.setTextColor(ContextCompat.getColor(context, R.color.channelflow_muted));
                time.setText(DateTimeExtensionsKt.getTimeFormatter(getContext()).format(program.getStartDate()));
                mInfoRow.addView(time);
            }
        }

        LiveTvPreferences liveTvPreferences = get(LiveTvPreferences.class);

        if (liveTvPreferences.get(LiveTvPreferences.Companion.getShowNewIndicator()) && BaseItemExtensionsKt.isNew(program) && (!liveTvPreferences.get(LiveTvPreferences.Companion.getShowPremiereIndicator()) || !Utils.isTrue(program.isPremiere()))) {
            addBlockText(context.getString(R.string.lbl_new), 10, Color.GRAY, R.drawable.dark_green_gradient);
        }

        if (liveTvPreferences.get(LiveTvPreferences.Companion.getShowPremiereIndicator()) && Utils.isTrue(program.isPremiere())) {
            addBlockText(context.getString(R.string.lbl_premiere), 10, Color.GRAY, R.drawable.dark_green_gradient);
        }

        if (liveTvPreferences.get(LiveTvPreferences.Companion.getShowRepeatIndicator()) && Utils.isTrue(program.isRepeat())) {
            addBlockText(context.getString(R.string.lbl_repeat), 10, Color.GRAY, R.color.channelflow_accent);
        }

        if (program.getOfficialRating() != null && !program.getOfficialRating().equals("0")) {
            addBlockText(program.getOfficialRating(), 10, Color.BLACK, R.drawable.block_text_bg);
        }

        if (liveTvPreferences.get(LiveTvPreferences.Companion.getShowHDIndicator()) && Utils.isTrue(program.isHd())) {
            addBlockText("HD", 10, Color.BLACK, R.drawable.block_text_bg);
        }

        mRecIndicator.setVisibility(View.GONE);


        if (keyListen) {
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mActivity.showProgramOptions();
                }
            });
        }

    }

    private void addBlockText(String text, int size, int textColor, int backgroundRes) {
        TextView view = new TextView(getContext());
        view.setTextSize(size);
        view.setTextColor(textColor);
        view.setText(" " + text + " ");
        view.setBackgroundResource(backgroundRes);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        params.setMargins(0, Utils.convertDpToPixel(getContext(), -2), 0, 0);
        view.setLayoutParams(params);
        mInfoRow.addView(view);
    }

    public void setCellBackground() {
        LiveTvPreferences liveTvPreferences = get(LiveTvPreferences.class);

        if (liveTvPreferences.get(LiveTvPreferences.Companion.getColorCodeGuide())) {
            if (Utils.isTrue(mProgram.isMovie())) {
                mBackgroundColor = getResources().getColor(R.color.guide_movie_bg);
            } else if (Utils.isTrue(mProgram.isNews())) {
                mBackgroundColor = getResources().getColor(R.color.guide_news_bg);
            } else if (Utils.isTrue(mProgram.isSports())) {
                mBackgroundColor = getResources().getColor(R.color.guide_sports_bg);
            } else if (Utils.isTrue(mProgram.isKids())) {
                mBackgroundColor = getResources().getColor(R.color.guide_kids_bg);
            }

            setBackgroundColor(mBackgroundColor);
        }
        applyGuideChrome(false);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (gainFocus) {
            applyGuideChrome(true);
            mActivity.setSelectedProgram(this);
        } else {
            applyGuideChrome(false);
        }
    }

    private void applyGuideChrome(boolean focused) {
        setBackground(org.jellyfin.androidtv.channelflow.ChannelFlowGuideChrome.INSTANCE.programBackground(getContext(), mProgram, focused));
        if (mProgramName != null) {
            mProgramName.setTextColor(ContextCompat.getColor(getContext(), focused
                    ? R.color.white
                    : R.color.channelflow_text));
        }
    }

    public BaseItemDto getProgram() { return mProgram; }

    public void setLast() { isLast = true; }
    public boolean isLast() { return isLast; }
    public void setFirst() { isFirst = true; }
    public boolean isFirst() { return isFirst; }
}

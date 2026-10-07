package com.simplecity.amp_library.lyrics;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.playback.MediaManager;
import com.simplecity.amp_library.utils.ViewUtils;

public class LyricsDialog implements LyricsView {

    private static final String TAG = "LyricsDialog";

    private Context context;

    private LyricsPresenter lyricsPresenter;

    private TextView lyricsTextView;

    private View noLyricsView;

    public LyricsDialog(MediaManager mediaManager) {
        lyricsPresenter = new LyricsPresenter(mediaManager);
    }

    public MaterialDialog getDialog(Context context) {

        this.context = context;

        @SuppressLint("InflateParams")
        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_lyrics, null);

        lyricsTextView = customView.findViewById(R.id.text1);

        noLyricsView = customView.findViewById(R.id.noLyricsView);

        lyricsPresenter.bindView(this);

        return new MaterialDialog.Builder(context)
                .customView(customView, false)
                .title(R.string.lyrics)
                .negativeText(R.string.close)
                .build();
    }

    @Override
    public void updateLyrics(@Nullable String lyrics) {
        lyricsTextView.setText(lyrics);
    }

    @Override
    public void showNoLyricsView(boolean show) {
        if (show) {
            ViewUtils.fadeOut(lyricsTextView, () -> {
                if (noLyricsView.getVisibility() == View.GONE) {
                    ViewUtils.fadeIn(noLyricsView, null);
                }
            });
        } else {
            ViewUtils.fadeOut(noLyricsView, () -> {
                if (lyricsTextView.getVisibility() == View.GONE) {
                    ViewUtils.fadeIn(lyricsTextView, null);
                }
            });
        }
    }

}
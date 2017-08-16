package com.simplecity.amp_library.lyrics;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.ViewUtils;

public class LyricsDialog implements LyricsView {

    private static final String TAG = "LyricsDialog";

    private Context context;

    private LyricsPresenter lyricsPresenter = new LyricsPresenter();

    private TextView lyricsTextView;

    private View noLyricsView;

    private View quickLyricInfo;

    public LyricsDialog() {
    }

    public MaterialDialog getDialog(Context context) {

        this.context = context;

        @SuppressLint("InflateParams")
        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_lyrics, null);

        lyricsTextView = customView.findViewById(R.id.text1);

        noLyricsView = customView.findViewById(R.id.noLyricsView);

        Button quickLyricButton = customView.findViewById(R.id.quickLyricButton);
        quickLyricButton.setText(QuickLyricUtils.getSpannedString());
        quickLyricButton.setOnClickListener(v -> lyricsPresenter.downloadOrLaunchQuickLyric());

        quickLyricInfo = customView.findViewById(R.id.quickLyricInfo);
        quickLyricInfo.setOnClickListener(v -> lyricsPresenter.showQuickLyricInfoDialog());

        View quickLyricsLayout = customView.findViewById(R.id.quickLyricLayout);
        if (!QuickLyricUtils.canDownloadQuickLyric()) {
            quickLyricsLayout.setVisibility(View.GONE);
        }

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

    @Override
    public void showQuickLyricInfoButton(boolean show) {
        quickLyricInfo.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void launchQuickLyric(@NonNull Song song) {
        QuickLyricUtils.getLyricsFor(context, song);
    }

    @Override
    public void downloadQuickLyric() {
        try {
            context.startActivity(QuickLyricUtils.getQuickLyricIntent());
        } catch (ActivityNotFoundException ignored) {
            // If the user doesn't have the play store on their device
        }
    }

    @Override
    public void showQuickLyricInfoDialog() {
        DialogUtils.getBuilder(context)
                .iconRes(R.drawable.quicklyric)
                .title(R.string.quicklyric)
                .content(context.getString(R.string.quicklyric_info))
                .positiveText(R.string.download)
                .onPositive((dialog, which) -> lyricsPresenter.downloadOrLaunchQuickLyric())
                .negativeText(R.string.close)
                .show();
    }
}
package com.simplecity.amp_library.lyrics;

import android.content.ActivityNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.fragments.BaseFragment;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecity.amp_library.utils.ViewUtils;

public class LyricsFragment extends BaseFragment implements LyricsView {

    private static final String TAG = "LyricsFragment";

    private LyricsPresenter lyricsPresenter = new LyricsPresenter();

    private TextView lyricsTextView;

    private View noLyricsView;

    private View quickLyricInfo;

    /**
     * Empty constructor as per the {@link android.app.Fragment} docs
     */
    public LyricsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_lyrics, container, false);
        rootView.setBackgroundColor(Color.parseColor("#C8000000"));

        lyricsTextView = (TextView) rootView.findViewById(R.id.text1);

        noLyricsView = rootView.findViewById(R.id.noLyricsView);

        Button quickLyricButton = (Button) rootView.findViewById(R.id.quickLyricButton);
        quickLyricButton.setText(QuickLyricUtils.getSpannedString());
        quickLyricButton.setOnClickListener(v -> lyricsPresenter.downloadOrLaunchQuickLyric());

        quickLyricInfo = rootView.findViewById(R.id.quickLyricInfo);
        quickLyricInfo.setOnClickListener(v -> lyricsPresenter.showQuickLyricInfoDialog());

        ScrollView scrollView = (ScrollView) rootView.findViewById(R.id.scrollView);

        final GestureDetector gestureDetector = new GestureDetector(this.getActivity(), new GestureListener());
        scrollView.setOnTouchListener((view, motionEvent) -> gestureDetector.onTouchEvent(motionEvent));

        ThemeUtils.themeScrollView(scrollView);

        View quickLyricsLayout = rootView.findViewById(R.id.quickLyricLayout);
        if (!QuickLyricUtils.canDownloadQuickLyric()) {
            quickLyricsLayout.setVisibility(View.GONE);
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        lyricsPresenter.bindView(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        lyricsPresenter.unbindView(this);
    }

    public void remove() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);
        ft.remove(LyricsFragment.this).commit();
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        GestureListener() {
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            remove();
            return true;
        }
    }

    @Override
    protected String screenName() {
        return TAG;
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
        QuickLyricUtils.getLyricsFor(getContext(), song);
    }

    @Override
    public void downloadQuickLyric() {
        try {
            startActivity(QuickLyricUtils.getQuickLyricIntent());
        } catch (ActivityNotFoundException ignored) {
            // If the user doesn't have the play store on their device
        }
    }

    @Override
    public void showQuickLyricInfoDialog() {
        DialogUtils.getBuilder(getContext())
                .iconRes(R.drawable.quicklyric)
                .title(R.string.quicklyric)
                .content(getString(R.string.quicklyric_info))
                .positiveText(R.string.download)
                .onPositive((dialog, which) -> lyricsPresenter.downloadOrLaunchQuickLyric())
                .negativeText(R.string.close)
                .show();
    }


}

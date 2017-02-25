package com.simplecity.amp_library.ui.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.simplecity.amp_library.model.Query;
import com.simplecity.amp_library.sql.SqlUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;

public class LyricsFragment extends BaseFragment {

    private static final String TAG = "LyricsFragment";

    private String mLyrics;
    private TextView mLyricsTextView;

    /**
     * Empty constructor as per the {@link android.app.Fragment} docs
     */
    public LyricsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_lyrics, container, false);
        rootView.setBackgroundColor(Color.parseColor("#C8000000"));
        mLyricsTextView = (TextView) rootView.findViewById(R.id.text1);

        final GestureDetector gestureDetector = new GestureDetector(this.getActivity(), new GestureListener());
        mLyricsTextView.setOnTouchListener((view, motionEvent) -> gestureDetector.onTouchEvent(motionEvent));


        Button quickLyricButton = (Button) rootView.findViewById(R.id.btn_quick_lyric);
        quickLyricButton.setOnClickListener(v -> {
            if (isQuickLyricInstalled()) {
                Intent intent = new Intent("com.geecko.QuickLyric.getLyrics");
                intent.putExtra("TAGS", new String[]{MusicUtils.getAlbumArtistName(), MusicUtils.getSongName()});
                startActivity(intent);
            }
        });
        if (isQuickLyricInstalled()) {
            quickLyricButton.setVisibility(View.VISIBLE);
        }

        ScrollView scrollView = (ScrollView) rootView.findViewById(R.id.scrollView);
        ThemeUtils.themeScrollView(scrollView);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLyrics = getLyrics();
        mLyricsTextView.setText(mLyrics);
    }

    public String getLyrics() {

        String lyrics = getActivity().getString(R.string.no_lyrics);
        String filePath = MusicUtils.getFilePath();

        if (filePath == null) {
            return lyrics;
        }

        if (filePath.startsWith("content://")) {
            String path = MusicUtils.getFilePath();
            if (path != null) {
                Query query = new Query.Builder()
                        .uri(Uri.parse(path))
                        .projection(new String[]{MediaStore.Audio.Media.DATA})
                        .build();
                Cursor cursor = SqlUtils.createQuery(getContext(), query);
                if (cursor != null) {
                    int colIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    if (cursor.moveToFirst()) {
                        filePath = cursor.getString(colIndex);
                    }
                    cursor.close();
                }
            }
        }

        File file = new File(filePath);
        if (file.exists()) {
            try {
                AudioFile audioFile = AudioFileIO.read(file);
                if (audioFile != null) {
                    Tag tag = audioFile.getTag();
                    if (tag != null) {
                        String tagLyrics = tag.getFirst(FieldKey.LYRICS);
                        if (tagLyrics != null && tagLyrics.length() != 0) {
                            lyrics = tagLyrics.replace("\r", "\n");
                        }
                    }
                }
            } catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException | UnsupportedOperationException ignored) {
            }
        }

        return lyrics;
    }

    public void updateLyrics() {

        mLyrics = getLyrics();
        if (mLyricsTextView != null) {
            mLyricsTextView.setText(mLyrics);
        }
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
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);
            ft.remove(LyricsFragment.this).commit();
            return true;
        }
    }

    private boolean isQuickLyricInstalled() {
        PackageManager pm = getActivity().getPackageManager();
        try {
            pm.getPackageInfo("com.geecko.QuickLyric", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}

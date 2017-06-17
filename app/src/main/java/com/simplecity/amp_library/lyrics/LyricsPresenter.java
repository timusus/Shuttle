package com.simplecity.amp_library.lyrics;

import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.f2prateek.rx.receivers.RxBroadcastReceiver;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Query;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.sql.SqlUtils;
import com.simplecity.amp_library.ui.presenters.Presenter;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.MusicUtils;

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

import rx.Observable;

class LyricsPresenter extends Presenter<LyricsView> {

    @Override
    public void bindView(@NonNull LyricsView view) {
        super.bindView(view);

        updateLyrics();

        addSubcscription(RxBroadcastReceiver.create(ShuttleApplication.getInstance(), new IntentFilter(MusicService.InternalIntents.META_CHANGED))
                .subscribe(intent -> updateLyrics(), error -> LogUtils.logException("LyricsPresenter: Error receiving meta changed", error)));
    }

    void downloadOrLaunchQuickLyric() {
        LyricsView lyricsView = getView();
        if (lyricsView != null) {
            if (QuickLyricUtils.isQLInstalled()) {
                Song song = MusicUtils.getSong();
                if (song != null) {
                    lyricsView.launchQuickLyric(song);
                }
            } else {
                lyricsView.downloadQuickLyric();
            }
        }
    }

    void showQuickLyricInfoDialog() {
        LyricsView lyricsView = getView();
        if (lyricsView != null) {
            lyricsView.showQuickLyricInfoDialog();
        }
    }

    private void updateLyrics() {

        addSubcscription(Observable.fromCallable(() -> {

            String lyrics = null;
            String path = MusicUtils.getFilePath();

            if (TextUtils.isEmpty(path)) {
                return null;
            }

            if (path.startsWith("content://")) {
                Query query = new Query.Builder()
                        .uri(Uri.parse(path))
                        .projection(new String[]{MediaStore.Audio.Media.DATA})
                        .build();

                Cursor cursor = SqlUtils.createQuery(ShuttleApplication.getInstance(), query);
                if (cursor != null) {
                    try {
                        int colIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                        if (cursor.moveToFirst()) {
                            path = cursor.getString(colIndex);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }

            File file = new File(path);
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
        }).subscribe(lyrics -> {
            LyricsView lyricsView = getView();
            if (lyricsView != null) {
                lyricsView.updateLyrics(lyrics);
                lyricsView.showNoLyricsView(lyrics == null);
                lyricsView.showQuickLyricInfoButton(!QuickLyricUtils.isQLInstalled());
            }
        }, error -> LogUtils.logException("LyricsPresenter: Error getting lyrics", error)));
    }
}
package com.simplecity.amp_library.lyrics;

import android.support.annotation.Nullable;

interface LyricsView {

    void updateLyrics(@Nullable String lyrics);

    void showNoLyricsView(boolean show);

    void showQuickLyricInfo();

    void launchQuickLyric();
}

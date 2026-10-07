package com.simplecity.amp_library.lyrics;

import androidx.annotation.Nullable;

interface LyricsView {

    void updateLyrics(@Nullable String lyrics);

    void showNoLyricsView(boolean show);
}

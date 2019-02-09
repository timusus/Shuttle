package com.simplecity.amp_library.ui.screens.lyrics

import com.simplecity.amp_library.model.Song

interface LyricsView {

    fun updateLyrics(lyrics: String?)

    fun showNoLyricsView(show: Boolean)

    fun showQuickLyricInfoButton(show: Boolean)

    fun showQuickLyricInfoDialog()

    fun downloadQuickLyric()

    fun launchQuickLyric(song: Song)
}
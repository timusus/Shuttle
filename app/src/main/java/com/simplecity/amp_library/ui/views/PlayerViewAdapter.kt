package com.simplecity.amp_library.ui.views

import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.screens.nowplaying.PlayerView

abstract class PlayerViewAdapter : PlayerView {

    override fun setSeekProgress(progress: Int) {

    }

    override fun currentTimeVisibilityChanged(visible: Boolean) {

    }

    override fun currentTimeChanged(seconds: Long) {

    }

    override fun totalTimeChanged(seconds: Long) {

    }

    override fun queueChanged(queuePosition: Int, queueLength: Int) {

    }

    override fun playbackChanged(isPlaying: Boolean) {

    }

    override fun shuffleChanged(shuffleMode: Int) {

    }

    override fun repeatChanged(repeatMode: Int) {

    }

    override fun favoriteChanged(isFavorite: Boolean) {

    }

    override fun trackInfoChanged(song: Song?) {

    }

    override fun showLyricsDialog() {

    }

    override fun showUpgradeDialog() {

    }

    override fun presentCreatePlaylistDialog(songs: List<Song>) {

    }

    override fun presentSongInfoDialog(song: Song) {

    }

    override fun onSongsAddedToPlaylist(playlist: Playlist, numSongs: Int) {

    }

    override fun onSongsAddedToQueue(numSongs: Int) {

    }

    override fun presentTagEditorDialog(song: Song) {

    }

    override fun presentDeleteDialog(songs: List<Song>) {

    }

    override fun shareSong(song: Song) {

    }

    override fun presentRingtonePermissionDialog() {

    }

    override fun showRingtoneSetMessage() {

    }
}
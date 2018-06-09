package com.simplecity.amp_library.playback

import android.net.Uri
import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.queue.QueueItem
import io.reactivex.Single
import io.reactivex.disposables.Disposable

interface MediaManager {

    companion object Defs {
        const val ADD_TO_PLAYLIST = 0
        const val PLAYLIST_SELECTED = 1
        const val NEW_PLAYLIST = 2
    }

    /**
     * Sends a list of songs to the MusicService for playback
     */
    fun playAll(songsSingle: Single<MutableList<Song>>, onEmpty: (String) -> Unit) : Disposable

    /**
     * Sends a list of songs to the MusicService for playback
     */
    fun playAll(songs: MutableList<Song>, position: Int, canClearShuffle: Boolean, onEmpty: (String) -> Unit)

    /**
     * Shuffles all songs in the given song list
     */
    fun shuffleAll(songsSingle: Single<List<Song>>, onEmpty: (String) -> Unit): Disposable

    /**
     * Shuffles all songs in the given list
     */
    fun shuffleAll(songs: MutableList<Song>, onEmpty: (String) -> Unit)

    /**
     * @param uri The source of the file
     */
    fun playFile(uri: Uri?)

    /**
     * @return [String] The path to the currently playing file
     */
    fun getFilePath(): String?

    /**
     * @return True if we're playing music, false otherwise.
     */
    fun isPlaying(): Boolean

    /**
     * @return The current shuffle mode
     */
    fun getShuffleMode(): Int

    /**
     * Sets the shuffle mode
     */
    fun setShuffleMode(mode: Int)

    /**
     * @return The current repeat mode
     */
    fun getRepeatMode(): Int

    /**
     * Changes to the next track
     */
    fun next()

    /**
     * Changes to the previous track
     *
     * @param allowTrackRestart if true, the track will restart if the track position is > 2 seconds
     */
    fun previous(allowTrackRestart: Boolean)

    /**
     * Plays or pauses the music depending on the current state.
     */
    fun playOrPause()

    fun getAudioSessionId(): Int

    /**
     * Note: This does not return a fully populated album artist.
     *
     * @return a partial [AlbumArtist] containing a partial [Album]
     * which contains the current song.
     */
    fun getAlbumArtist(): AlbumArtist?

    /**
     * Note: This does not return a fully populated album.
     *
     * @return a partial [Album] containing this song.
     */
    fun getAlbum(): Album?

    fun getSong(): Song?

    fun getGenre(): Single<Genre>

    /**
     * Method getPosition.
     *
     * @return [Long]
     */
    fun getPosition(): Long

    /**
     * Method duration.
     *
     * @return [Long]
     */
    fun getDuration(): Long

    /**
     * Method seekTo.
     *
     * @param position the [Long] position to seek to
     */
    fun seekTo(position: Long)

    fun moveQueueItem(from: Int, to: Int)

    fun toggleShuffleMode()

    fun cycleRepeat()

    fun addToQueue(songs: MutableList<Song>, onAdded: (String) -> Unit)

    fun playNext(songsSingle: Single<List<Song>>, onAdded: (String) -> Unit): Disposable?

    fun playNext(songs: MutableList<Song>, onAdded: (String) -> Unit)

    fun moveToNext(queueItem: QueueItem)

    fun setQueuePosition(position: Int)

    fun clearQueue()

    fun getQueue(): MutableList<QueueItem>

    fun getQueuePosition(): Int

    fun removeFromQueue(queueItem: QueueItem)

    fun removeFromQueue(queueItems: List<QueueItem>)

    fun removeSongsFromQueue(songs: MutableList<Song>)

    fun toggleFavorite()

    fun closeEqualizerSessions(internal: Boolean, audioSessionId: Int)

    fun openEqualizerSession(internal: Boolean, audioSessionId: Int)

    fun updateEqualizer()
}
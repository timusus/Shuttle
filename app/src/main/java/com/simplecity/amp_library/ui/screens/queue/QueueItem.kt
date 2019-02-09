package com.simplecity.amp_library.ui.screens.queue

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import com.simplecity.amp_library.model.Song

class QueueItem(var song: Song, var occurrence: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueueItem

        if (song != other.song) return false
        if (occurrence != other.occurrence) return false

        return true
    }

    override fun hashCode(): Int {
        var result = song.hashCode()
        result = 31 * result + occurrence
        return result
    }
}

fun List<Song>.toQueueItems(): List<QueueItem> {
    val queueItems = map { song -> QueueItem(song, 1) }
    queueItems.updateOccurrence()
    return queueItems
}

fun List<QueueItem>.updateOccurrence() {
    groupBy { queueItem -> queueItem.song }
        .values
        .forEach {
            it.forEachIndexed { index, queueItem ->
                queueItem.occurrence = index + 1
            }
        }
}

fun List<QueueItem>.toSongs(): List<Song> {
    return map { queueItem -> queueItem.song }
}

fun QueueItem.toMediaSessionQueueItem(): MediaSessionCompat.QueueItem {
    val mediaDescription = MediaDescriptionCompat.Builder()
        .setMediaId(song.id.toString())
        .setTitle(song.name)
        .setSubtitle(song.artistName)
        .build()
    return MediaSessionCompat.QueueItem(mediaDescription, hashCode().toLong())
}

fun List<QueueItem>.toMediaSessionQueueItems(): List<MediaSessionCompat.QueueItem> {
    return map { queueItem -> queueItem.toMediaSessionQueueItem() }
}
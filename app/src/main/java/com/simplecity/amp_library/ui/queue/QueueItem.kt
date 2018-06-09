package com.simplecity.amp_library.ui.queue

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
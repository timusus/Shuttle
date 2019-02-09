package com.simplecity.amp_library.playback

import android.content.Context
import android.content.Intent
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.constants.ExternalIntents

class ScrobbleManager(private val playbackSettingsManager: PlaybackSettingsManager) {

    enum class ScrobbleStatus(val value: Int) {
        START(0),
        RESUME(1),
        PAUSE(2),
        COMPLETE(3)
    }

    fun scrobbleBroadcast(context: Context, state: ScrobbleStatus, song: Song) {
        if (playbackSettingsManager.enableLastFmScrobbling) {
            val intent = Intent(ExternalIntents.SCROBBLER)
            intent.putExtra("state", state.value)
            intent.putExtra("app-name", context.getString(R.string.app_name))
            intent.putExtra("app-package", context.packageName)
            intent.putExtra("artist", song.artistName)
            intent.putExtra("album", song.albumName)
            intent.putExtra("track", song.name)
            intent.putExtra("duration", song.duration / 1000)
            context.sendBroadcast(intent)
        }
    }
}

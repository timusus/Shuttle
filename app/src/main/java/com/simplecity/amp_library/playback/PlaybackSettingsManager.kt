package com.simplecity.amp_library.playback

import android.content.SharedPreferences
import com.simplecity.amp_library.utils.BaseSettingsManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackSettingsManager @Inject constructor(sharedPreferences: SharedPreferences) : BaseSettingsManager(sharedPreferences) {

    private val KEY_SEEK_POSITION = "seek_position"
    var seekPosition: Long
        get() = getLong(KEY_SEEK_POSITION, 0)
        set(seekPosition) = setLong(KEY_SEEK_POSITION, seekPosition)

    private val KEY_QUEUE_POSITION = "queue_position"
    var queuePosition: Int
        get() = getInt(KEY_QUEUE_POSITION, 0)
        set(queuePosition) = setInt(KEY_QUEUE_POSITION, queuePosition)

    private val KEY_REPEAT_MODE = "repeat_mode"
    @QueueManager.RepeatMode()
    var repeatMode: Int
        get() = getInt(KEY_REPEAT_MODE, QueueManager.RepeatMode.OFF)
        set(repeatMode) = setInt(KEY_REPEAT_MODE, repeatMode)

    private val KEY_SHUFFLE_MODE = "shuffle_mode"
    @QueueManager.ShuffleMode()
    var shuffleMode: Int
        get() = getInt(KEY_SHUFFLE_MODE, QueueManager.ShuffleMode.OFF)
        set(shuffleMode) = setInt(KEY_SHUFFLE_MODE, shuffleMode)

    private val KEY_QUEUE_LIST = "queue_list"
    var queueList: String?
        get() = getString(KEY_QUEUE_LIST, null)
        set(queueList) = setString(KEY_QUEUE_LIST, queueList)

    private val KEY_SHUFFLE_LIST = "shuffle_list"
    var shuffleList: String?
        get() = getString(KEY_SHUFFLE_LIST, null)
        set(shuffleList) = setString(KEY_SHUFFLE_LIST, shuffleList)

    private val KEY_HEADSET_DISCONNECT = "pref_headset_disconnect"
    var pauseOnHeadsetDisconnect: Boolean
        get() = getBool(KEY_HEADSET_DISCONNECT, true)
        set(headsetDisconnect) = setBool(KEY_HEADSET_DISCONNECT, headsetDisconnect)

    private val KEY_HEADSET_CONNECT = "pref_headset_connect"
    var playOnHeadsetConnect: Boolean
        get() = getBool(KEY_HEADSET_CONNECT, false)
        set(headsetConnect) = setBool(KEY_HEADSET_CONNECT, headsetConnect)

    private val KEY_LAST_FM_SCROBBLING = "pref_simple_lastfm_scrobbler"
    var enableLastFmScrobbling: Boolean
        get() = getBool(KEY_LAST_FM_SCROBBLING, false)
        set(enableLastFmScrobbling) = setBool(KEY_LAST_FM_SCROBBLING, enableLastFmScrobbling)

}

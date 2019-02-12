package com.simplecity.amp_library.ui.screens.shortcut

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.simplecity.amp_library.playback.MusicService
import com.simplecity.amp_library.playback.constants.ShortcutCommands
import com.simplecity.amp_library.ui.screens.main.MainActivity
import com.simplecity.amp_library.utils.AnalyticsManager
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.ResumingServiceManager
import com.simplecity.amp_library.utils.playlists.FavoritesPlaylistManager
import com.simplecity.amp_library.utils.playlists.PlaylistManager
import dagger.android.AndroidInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class ShortcutTrampolineActivity : AppCompatActivity() {

    @Inject lateinit var favoritesPlaylistManager: FavoritesPlaylistManager

    @Inject lateinit var analyticsManager: AnalyticsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        val action = intent.action
        when (action) {
            ShortcutCommands.PLAY, ShortcutCommands.SHUFFLE_ALL -> {
                val intent = Intent(this, MusicService::class.java)
                intent.action = action
                ResumingServiceManager(lifecycle, analyticsManager).startService(this, intent, null)
                finish()
            }
            ShortcutCommands.FOLDERS -> {
                intent = Intent(this, MainActivity::class.java)
                intent.action = action
                startActivity(intent)
                finish()
            }
            ShortcutCommands.PLAYLIST -> {
                intent = Intent(this, MainActivity::class.java)
                intent.action = action
                favoritesPlaylistManager.getFavoritesPlaylist()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { playlist ->
                                    intent.putExtra(PlaylistManager.ARG_PLAYLIST, playlist)
                                    startActivity(intent)
                                    finish()
                                },
                                { error -> LogUtils.logException(TAG, "Error starting activity", error) }
                        )
            }
        }
    }

    companion object {
        private const val TAG = "ShortcutTrampolineActiv"
    }
}

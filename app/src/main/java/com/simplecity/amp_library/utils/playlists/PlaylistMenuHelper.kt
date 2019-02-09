package com.simplecity.amp_library.utils.playlists

import android.content.Intent
import android.view.SubMenu
import com.simplecity.amp_library.R
import com.simplecity.amp_library.data.PlaylistsRepository
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.utils.LogUtils
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class PlaylistMenuHelper @Inject constructor(
    private val playlistsRepository: PlaylistsRepository
) {

    fun createPlaylistMenu(subMenu: SubMenu): Disposable {
        return createPlaylistMenu(subMenu, false)
            .subscribe(
                { },
                { throwable -> LogUtils.logException(TAG, "createPlaylistMenu error", throwable) }
            )
    }

    fun createUpdatingPlaylistMenu(subMenu: SubMenu): Completable {
        return createPlaylistMenu(subMenu, true)
    }

    fun createPlaylistMenu(subMenu: SubMenu, autoUpdate: Boolean): Completable {
        return playlistsRepository.getPlaylists()
            .take(if (autoUpdate) java.lang.Long.MAX_VALUE else 1)
            .doOnNext { playlists ->
                subMenu.clear()
                subMenu.add(0, MediaManager.Defs.NEW_PLAYLIST, 0, R.string.new_playlist)
                for (playlist in playlists) {
                    val intent = Intent()
                    intent.putExtra(PlaylistManager.ARG_PLAYLIST, playlist)
                    subMenu.add(0, MediaManager.Defs.PLAYLIST_SELECTED, 0, playlist.name).intent = intent
                }
            }
            .ignoreElements()
            .doOnError { throwable -> LogUtils.logException(TAG, "createUpdatingPlaylistMenu failed", throwable) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    companion object {

        private const val TAG = "PlaylistMenuHelper"
    }
}

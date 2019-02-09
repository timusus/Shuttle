package com.simplecity.amp_library.ui.screens.drawer

import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.data.Repository
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.ui.common.PurchasePresenter
import com.simplecity.amp_library.ui.screens.playlist.menu.PlaylistMenuContract
import com.simplecity.amp_library.ui.screens.playlist.menu.PlaylistMenuPresenter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.PermissionUtils
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.ShuttleUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DrawerPresenter @Inject
constructor(
    private val application: ShuttleApplication,
    private val navigationEventRelay: NavigationEventRelay,
    private val songsRepository: Repository.SongsRepository,
    private val playlistsRepository: Repository.PlaylistsRepository,
    private val settingsManager: SettingsManager,
    private val playlistMenuPresenter: PlaylistMenuPresenter
) : PurchasePresenter<DrawerView>(),
    PlaylistMenuContract.Presenter by playlistMenuPresenter {

    override fun bindView(view: DrawerView) {
        super.bindView(view)

        playlistMenuPresenter.bindView(view)

        loadData(view)

        addDisposable(navigationEventRelay.events
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { drawerEvent ->
                val drawerView = getView()
                when (drawerEvent.type) {
                    NavigationEventRelay.NavigationEvent.Type.LIBRARY_SELECTED -> drawerView?.setDrawerItemSelected(DrawerParent.Type.LIBRARY)
                    NavigationEventRelay.NavigationEvent.Type.FOLDERS_SELECTED -> if (drawerView != null) {
                        if (ShuttleUtils.isUpgraded(application, settingsManager)) {
                            drawerView.setDrawerItemSelected(DrawerParent.Type.FOLDERS)
                        } else {
                            upgradeClicked()
                        }
                    }
                }
            })
    }

    override fun unbindView(view: DrawerView) {
        super.unbindView(view)

        playlistMenuPresenter.unbindView(view)
    }

    private fun loadData(drawerView: DrawerView) {
        PermissionUtils.RequestStoragePermissions {
            addDisposable(playlistsRepository.getAllPlaylists(songsRepository)
                .observeOn(AndroidSchedulers.mainThread())
                // Delay the subscription so we're not querying data while the app is launching
                .delaySubscription(Observable.timer(1500, TimeUnit.MILLISECONDS))
                // after all, clear all playlist item
                // to avoid memory leak in static var DrawerParent.playlistsParent
                .doFinally { drawerView.setPlaylistItems(emptyList()) }
                .subscribe(
                    { drawerView.setPlaylistItems(it) },
                    { error -> LogUtils.logException(TAG, "Error refreshing DrawerFragment adapter items", error) }
                ))
        }
    }

    internal fun onDrawerItemClicked(drawerParent: DrawerParent) {
        val drawerView = view
        if (drawerView != null && drawerParent.isSelectable) {
            drawerView.setDrawerItemSelected(drawerParent.type)
        }

        closeDrawer()

        if (drawerParent.navigationEvent != null) {
            navigationEventRelay.sendEvent(drawerParent.navigationEvent!!)
        }
    }

    private fun closeDrawer() {
        val drawerView = view
        drawerView?.closeDrawer()
    }

    fun onPlaylistClicked(playlist: Playlist) {
        closeDrawer()
        navigationEventRelay.sendEvent(NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.PLAYLIST_SELECTED, playlist))
    }

    companion object {

        private const val TAG = "DrawerPresenter"
    }
}
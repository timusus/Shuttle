package com.simplecity.amp_library.ui.screens.drawer

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.aesthetic.Rx
import com.bignerdranch.expandablerecyclerview.model.Parent
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.simplecity.amp_library.R
import com.simplecity.amp_library.billing.BillingManager
import com.simplecity.amp_library.data.Repository
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.common.BaseFragment
import com.simplecity.amp_library.ui.dialog.UpgradeDialog
import com.simplecity.amp_library.ui.dialog.WeekSelectorDialog
import com.simplecity.amp_library.ui.screens.nowplaying.PlayerPresenter
import com.simplecity.amp_library.ui.screens.playlist.dialog.DeletePlaylistConfirmationDialog
import com.simplecity.amp_library.ui.screens.playlist.dialog.M3uPlaylistDialog
import com.simplecity.amp_library.ui.screens.playlist.dialog.RenamePlaylistDialog
import com.simplecity.amp_library.ui.views.PlayerViewAdapter
import com.simplecity.amp_library.utils.LogUtils
import com.simplecity.amp_library.utils.PlaceholderProvider
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.SleepTimer
import com.simplecity.amp_library.utils.menu.playlist.PlaylistMenuUtils
import com.simplecity.amp_library.utils.playlists.FavoritesPlaylistManager
import com.simplecity.amp_library.utils.playlists.PlaylistManager
import dagger.android.support.AndroidSupportInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.drawer_header.artist_image
import kotlinx.android.synthetic.main.drawer_header.background_image
import kotlinx.android.synthetic.main.drawer_header.line1
import kotlinx.android.synthetic.main.drawer_header.line2
import kotlinx.android.synthetic.main.drawer_header.placeholder_text
import kotlinx.android.synthetic.main.fragment_drawer.recyclerView
import java.util.ArrayList
import javax.inject.Inject

class DrawerFragment : BaseFragment(), DrawerView, View.OnCreateContextMenuListener, DrawerParent.ClickListener {

    private lateinit var adapter: DrawerAdapter

    private var drawerLayout: DrawerLayout? = null

    private var playlistDrawerParent: DrawerParent? = null

    @DrawerParent.Type
    private var selectedDrawerParent = DrawerParent.Type.LIBRARY

    private var currentSelectedPlaylist: Playlist? = null

    @Inject lateinit var playerPresenter: PlayerPresenter

    @Inject lateinit var drawerPresenter: DrawerPresenter

    @Inject lateinit var billingManager: BillingManager

    @Inject lateinit var songsRepository: Repository.SongsRepository

    @Inject lateinit var playlistsRepository: Repository.PlaylistsRepository

    @Inject lateinit var settingsManager: SettingsManager

    @Inject lateinit var requestManager: RequestManager

    @Inject lateinit var playlistManager: PlaylistManager

    @Inject lateinit var favoritesPlaylistManager: FavoritesPlaylistManager

    private var backgroundPlaceholder: Drawable? = null

    private val disposables = CompositeDisposable()

    private var drawerParents: MutableList<Parent<DrawerChild>>? = null

    // Lifecycle

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            selectedDrawerParent = savedInstanceState.getInt(STATE_SELECTED_DRAWER_PARENT, DrawerParent.Type.LIBRARY)
            currentSelectedPlaylist = savedInstanceState.get(STATE_SELECTED_PLAYLIST) as Playlist?
        }

        backgroundPlaceholder = ContextCompat.getDrawable(context!!, R.drawable.ic_drawer_header_placeholder)

        playlistDrawerParent = DrawerParent.getPlaylistsParent(settingsManager)

        drawerParents = ArrayList()
        drawerParents!!.add(DrawerParent.getLibraryParent(settingsManager))
        drawerParents!!.add(DrawerParent.getFolderParent(context!!, settingsManager))
        drawerParents!!.add(playlistDrawerParent!!)
        drawerParents!!.add(DrawerDivider())
        drawerParents!!.add(DrawerParent.getSleepTimerParent(settingsManager))
        drawerParents!!.add(DrawerParent.getEqualizerParent(settingsManager))
        drawerParents!!.add(DrawerParent.getSettingsParent(settingsManager))
        drawerParents!!.add(DrawerParent.getSupportParent(settingsManager))

        adapter = DrawerAdapter(drawerParents!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_drawer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView!!.layoutManager = LinearLayoutManager(context)
        recyclerView!!.adapter = adapter

        setDrawerItemSelected(selectedDrawerParent)

        drawerPresenter.bindView(this)
        playerPresenter.bindView(playerViewAdapter)

        drawerLayout = getParentDrawerLayout(view)
    }

    override fun onResume() {
        super.onResume()

        // Todo: Move this crap to presenter
        disposables.add(Aesthetic.get(context)
            .colorPrimary()
            .compose(Rx.distinctToMainThread())
            .subscribe { color ->
                backgroundPlaceholder!!.setColorFilter(color!!, PorterDuff.Mode.MULTIPLY)
                if (mediaManager.song == null) {
                    background_image.setImageDrawable(backgroundPlaceholder)
                }
            })

        playerPresenter.updateTrackInfo()

        disposables.add(
            SleepTimer.getInstance().currentTimeObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ aLong ->
                    drawerParents!!
                        .forEachIndexed { i, drawerParent ->
                            if (aLong > 0 && drawerParent is DrawerParent && drawerParent.type == DrawerParent.Type.SLEEP_TIMER) {
                                drawerParent.setTimeRemaining(aLong!!)
                                adapter.notifyParentChanged(i)
                            }
                        }
                }, { throwable -> LogUtils.logException(TAG, "Error observing sleep time", throwable) })
        )

        disposables.add(
            SleepTimer.getInstance().timerActiveSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ active ->
                    drawerParents!!
                        .forEachIndexed { i, drawerParent ->
                            if (drawerParent is DrawerParent && drawerParent.type == DrawerParent.Type.SLEEP_TIMER) {
                                drawerParent.setTimerActive(active!!)
                                adapter.notifyParentChanged(i)
                            }
                        }
                },
                    { throwable -> LogUtils.logException(TAG, "Error observing sleep state", throwable) })
        )

        drawerParents!!
            .filter { parent -> parent is DrawerParent }
            .forEach { parent -> (parent as DrawerParent).setListener(this) }
    }

    override fun onPause() {
        disposables.clear()

        drawerParents!!
            .filter { parent -> parent is DrawerParent }
            .forEach { parent -> (parent as DrawerParent).setListener(null) }

        super.onPause()
    }

    override fun onDestroyView() {
        drawerPresenter.unbindView(this)
        playerPresenter.unbindView(playerViewAdapter)

        super.onDestroyView()
    }

    private val playerViewAdapter: PlayerViewAdapter = object : PlayerViewAdapter() {
        override fun trackInfoChanged(song: Song?) {

            if (song == null) {
                return
            }

            line1.text = song.name
            line2.text = String.format("%s - %s", song.albumArtistName, song.albumName)
            placeholder_text.setText(R.string.app_name)

            requestManager.load(song)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .error(backgroundPlaceholder)
                .into(background_image)

            requestManager.load<AlbumArtist>(song.albumArtist)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(PlaceholderProvider.getInstance(context).mediumPlaceHolderResId)
                .into(artist_image)

            if (song.name == null || song.albumName == null && song.albumArtistName == null) {
                placeholder_text.visibility = View.VISIBLE
                line1.visibility = View.GONE
                line2.visibility = View.GONE
            } else {
                placeholder_text.visibility = View.GONE
                line1.visibility = View.VISIBLE
                line2.visibility = View.VISIBLE
            }
        }
    }

    override fun onClick(drawerParent: DrawerParent) {
        drawerPresenter.onDrawerItemClicked(drawerParent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(STATE_SELECTED_DRAWER_PARENT, selectedDrawerParent)
        outState.putSerializable(STATE_SELECTED_PLAYLIST, currentSelectedPlaylist)
    }

    internal fun onPlaylistClicked(playlist: Playlist) {
        drawerPresenter.onPlaylistClicked(playlist)
    }

    override fun setPlaylistItems(playlists: List<Playlist>) {

        val parentPosition = adapter.parentList.indexOf(playlistDrawerParent)

        val prevItemCount = playlistDrawerParent!!.children.size
        playlistDrawerParent!!.children.clear()
        adapter.notifyChildRangeRemoved(parentPosition, 0, prevItemCount)

        val drawerChildren = playlists
            .map { playlist ->
                val drawerChild = DrawerChild(playlist)
                drawerChild.setListener(object : DrawerChild.ClickListener {
                    override fun onClick(playlist: Playlist) {
                        onPlaylistClicked(playlist)
                    }

                    override fun onOverflowClick(view: View, playlist: Playlist) {
                        val popupMenu = PopupMenu(view.context, view)
                        PlaylistMenuUtils.setupPlaylistMenu(popupMenu, playlist)
                        popupMenu.setOnMenuItemClickListener(PlaylistMenuUtils.getPlaylistPopupMenuClickListener(playlist, drawerPresenter))
                        popupMenu.show()
                    }
                })
                drawerChild
            }.toList()

        playlistDrawerParent!!.children.addAll(drawerChildren)
        adapter.notifyChildRangeInserted(parentPosition, 0, drawerChildren.size)

        adapter.notifyParentChanged(parentPosition)
    }

    override fun closeDrawer() {
        drawerLayout?.closeDrawer(Gravity.START)
    }

    override fun setDrawerItemSelected(@DrawerParent.Type type: Int) {
        adapter.parentList
            .forEachIndexed { i, drawerParent ->
                if (drawerParent is DrawerParent) {
                    if (drawerParent.type == type) {
                        if (!drawerParent.isSelected) {
                            drawerParent.isSelected = true
                            adapter.notifyParentChanged(i)
                        }
                    } else {
                        if (drawerParent.isSelected) {
                            drawerParent.isSelected = false
                            adapter.notifyParentChanged(i)
                        }
                    }
                }
            }
    }

    override fun showUpgradeDialog() {
        UpgradeDialog().show(childFragmentManager)
    }

    // PlaylistMenuContract.View Implementation

    override fun onPlaybackFailed() {
        // Todo: Improve error message
        Toast.makeText(context, R.string.empty_playlist, Toast.LENGTH_SHORT).show()
    }

    override fun presentEditDialog(playlist: Playlist) {
        WeekSelectorDialog().show(childFragmentManager)
    }

    override fun presentRenameDialog(playlist: Playlist) {
        RenamePlaylistDialog.newInstance(playlist).show(childFragmentManager)
    }

    override fun presentM3uDialog(playlist: Playlist) {
        M3uPlaylistDialog.newInstance(playlist).show(childFragmentManager)
    }

    override fun presentDeletePlaylistDialog(playlist: Playlist) {
        DeletePlaylistConfirmationDialog.newInstance(playlist).show(childFragmentManager)
    }

    override fun onSongsAddedToQueue(numSongs: Int) {
        Toast.makeText(context, context!!.resources.getQuantityString(R.plurals.NNNtrackstoqueue, numSongs, numSongs), Toast.LENGTH_SHORT).show()
    }

    // BaseDetailFragment Implementation

    override fun screenName(): String {
        return TAG
    }

    // Static

    companion object {

        private const val TAG = "DrawerFragment"

        private const val STATE_SELECTED_DRAWER_PARENT = "selected_drawer_parent"

        private const val STATE_SELECTED_PLAYLIST = "selected_drawer_playlist"

        fun getParentDrawerLayout(v: View?): DrawerLayout? {
            if (v == null) return null

            if (v is DrawerLayout) {
                return v
            }

            return if (v.parent is View) {
                getParentDrawerLayout(v.parent as View)
            } else null
        }
    }
}

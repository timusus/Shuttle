package com.simplecity.amp_library.ui.screens.queue

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.aesthetic.Util
import com.bumptech.glide.RequestManager
import com.simplecity.amp_library.R
import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.billing.BillingManager
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager.Defs
import com.simplecity.amp_library.ui.common.BaseFragment
import com.simplecity.amp_library.ui.dialog.DeleteDialog
import com.simplecity.amp_library.ui.dialog.SongInfoDialog
import com.simplecity.amp_library.ui.dialog.UpgradeDialog
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel
import com.simplecity.amp_library.ui.modelviews.SubheaderView
import com.simplecity.amp_library.ui.screens.nowplaying.PlayerPresenter
import com.simplecity.amp_library.ui.screens.playlist.dialog.CreatePlaylistDialog
import com.simplecity.amp_library.ui.screens.tagger.TaggerDialog
import com.simplecity.amp_library.ui.views.ContextualToolbar
import com.simplecity.amp_library.ui.views.LockActionBarView
import com.simplecity.amp_library.ui.views.PlayerViewAdapter
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetSlideEventRelay
import com.simplecity.amp_library.utils.*
import com.simplecity.amp_library.utils.ContextualToolbarHelper.Callback
import com.simplecity.amp_library.utils.extensions.share
import com.simplecity.amp_library.utils.menu.queue.QueueMenuUtils
import com.simplecity.amp_library.utils.menu.queue.removeQueueItem
import com.simplecity.amp_library.utils.playlists.PlaylistMenuHelper
import com.simplecity.amp_library.utils.sorting.SortManager
import com.simplecity.multisheetview.ui.view.MultiSheetView
import com.simplecity.multisheetview.ui.view.MultiSheetView.Sheet
import com.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter
import com.simplecityapps.recycler_adapter.model.ViewModel
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_queue.*
import java.util.*
import javax.inject.Inject

class QueueFragment :
        BaseFragment(),
        QueueContract.View {

    private var loadDataDisposable: Disposable? = null

    private val disposables = CompositeDisposable()

    private lateinit var adapter: ViewModelAdapter

    @Inject
    lateinit var application: ShuttleApplication

    @Inject
    lateinit var requestManager: RequestManager

    @Inject
    lateinit var multiSheetSlideEventRelay: MultiSheetSlideEventRelay

    @Inject
    lateinit var playerPresenter: PlayerPresenter

    @Inject
    lateinit var queuePresenter: QueuePresenter

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var playlistMenuHelper: PlaylistMenuHelper

    @Inject
    lateinit var sortManager: SortManager

    private lateinit var itemTouchHelper: ItemTouchHelper

    private lateinit var itemTouchHelperCallback: com.simplecity.amp_library.ui.views.recyclerview.ItemTouchHelperCallback

    private lateinit var cabToolbar: ContextualToolbar

    private lateinit var cabHelper: ContextualToolbarHelper<QueueItem>


    // Lifecycle

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        adapter = ViewModelAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_queue, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener { v -> activity?.onBackPressed() }
        toolbar.inflateMenu(R.menu.menu_fragment_queue)

        val subMenu = toolbar.menu.addSubMenu(0, Defs.ADD_TO_PLAYLIST, 1, R.string.save_as_playlist)
        disposables.add(playlistMenuHelper.createUpdatingPlaylistMenu(subMenu).subscribe())

        toolbar.setOnMenuItemClickListener(toolbarListener)

        cabToolbar = contextualToolbar as ContextualToolbar

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setRecyclerListener(RecyclerListener())
        recyclerView.adapter = adapter

        itemTouchHelperCallback = ItemTouchHelperCallback()
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        val lockMenuItem = toolbar.menu.findItem(R.id.menu_lock)
        val menuActionView = lockMenuItem.actionView as LockActionBarView
        val swipeLocked = settingsManager.queueSwipeLocked()

        setQueueSwipeLocked(swipeLocked)
        menuActionView.setLocked(swipeLocked, false)

        menuActionView.setOnClickListener { lockActionBarView ->
            (lockActionBarView as LockActionBarView).toggle()
            queuePresenter.setQueueSwipeLocked(lockActionBarView.isLocked)
        }

        disposables.add(Aesthetic.get(context)
                .colorPrimary()
                .subscribe { color ->
                    val isLight = Util.isColorLight(color!!)
                    line1.setTextColor(if (isLight) Color.BLACK else Color.WHITE)
                    line2.setTextColor(if (isLight) Color.BLACK else Color.WHITE)
                })

        // In landscape, we need to adjust the status bar's translation depending on the slide offset of the sheet
        if (ShuttleUtils.isLandscape(application)) {
            statusBarView.translationY = ResourceUtils.toPixels(16f).toFloat()

            disposables.add(multiSheetSlideEventRelay.events
                    .filter { multiSheetEvent -> multiSheetEvent.sheet == MultiSheetView.Sheet.SECOND }
                    .filter { multiSheetEvent -> multiSheetEvent.slideOffset >= 0 }
                    .subscribe { multiSheetEvent -> statusBarView.translationY = (1 - multiSheetEvent.slideOffset) * ResourceUtils.toPixels(16f) })
        }

        setupContextualToolbar()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyItemRangeChanged(0, adapter.itemCount, 0)

        playerPresenter.bindView(playerViewAdapter)
        queuePresenter.bindView(this)
    }

    override fun onPause() {
        super.onPause()
        loadDataDisposable?.dispose()
        playerPresenter.unbindView(playerViewAdapter)
        queuePresenter.unbindView(this)
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    private val songClickListener = object : QueueViewBinder.ClickListener {

        override fun onQueueItemClick(position: Int, queueViewBinder: QueueViewBinder) {
            if (!cabHelper.handleClick(queueViewBinder, queueViewBinder.queueItem)) {
                queuePresenter.play(queueViewBinder.queueItem)
            }
        }

        override fun onQueueItemLongClick(position: Int, queueViewBinder: QueueViewBinder): Boolean {
            return cabHelper.handleLongClick(queueViewBinder, queueViewBinder.queueItem)
        }

        override fun onQueueItemOverflowClick(position: Int, view: View, queueViewBinder: QueueViewBinder) {
            val menu = PopupMenu(view.context, view)
            QueueMenuUtils.setupQueueSongMenu(menu, playlistMenuHelper)
            menu.setOnMenuItemClickListener(QueueMenuUtils.getQueueMenuClickListener(queueViewBinder.queueItem, queuePresenter))
            menu.show()
        }

        override fun onStartDrag(holder: QueueViewBinder.ViewHolder) {
            itemTouchHelper.startDrag(holder)
        }
    }

    private val playerViewAdapter = object : PlayerViewAdapter() {
        override fun trackInfoChanged(song: Song?) {
            if (song != null) {
                line1.text = song.name
                if (song.albumArtistName != null && song.albumName != null) {
                    line2.text = String.format("%s • %s", song.albumArtistName, song.albumName)
                }
            }
        }

        override fun showUpgradeDialog() {
            UpgradeDialog().show(childFragmentManager)
        }
    }

    private var toolbarListener = Toolbar.OnMenuItemClickListener { item ->
        when (item.itemId) {
            R.id.menu_clear -> {
                queuePresenter.clearQueue()
                return@OnMenuItemClickListener true
            }
            Defs.NEW_PLAYLIST -> {
                queuePresenter.saveQueue(context!!)
                return@OnMenuItemClickListener true
            }
            Defs.PLAYLIST_SELECTED -> {
                queuePresenter.saveQueue(context!!, item)
                return@OnMenuItemClickListener true
            }
        }
        false
    }

    private fun setupContextualToolbar() {
        cabToolbar.menu.clear()
        cabToolbar.inflateMenu(R.menu.context_menu_queue)

        val sub = cabToolbar.menu.findItem(R.id.queue_add_to_playlist).subMenu
        disposables.add(playlistMenuHelper.createUpdatingPlaylistMenu(sub).subscribe())
        cabToolbar.setOnMenuItemClickListener(QueueMenuUtils.getQueueMenuClickListener(Single.fromCallable { cabHelper.items }, queuePresenter) { cabHelper.finish() })

        cabHelper = ContextualToolbarHelper(context, cabToolbar, object : Callback {
            override fun notifyItemChanged(viewModel: SelectableViewModel) {

                val index = adapter.items.indexOf(viewModel as ViewModel<*>)
                if (index >= 0) {
                    adapter.notifyItemChanged(index, 0)
                }
            }

            override fun notifyDatasetChanged() {
                adapter.notifyItemRangeChanged(0, adapter.items.size, 0)
            }
        })
    }

    override fun screenName(): String {
        return TAG
    }


    // QueueView implementation

    override fun setData(queueItems: List<QueueItem>, position: Int) {

        PermissionUtils.RequestStoragePermissions {
            if (activity != null && isAdded) {
                loadDataDisposable?.dispose()

                val queueHeaderView = QueueHeaderView(
                        StringUtils.makeSongsAndTimeLabel(
                                application,
                                queueItems.size,
                                queueItems.map { queueItem -> queueItem.song.duration / 1000 }.sum()
                        )
                )

                val viewModels = ArrayList<ViewModel<*>>()
                viewModels.add(queueHeaderView)

                viewModels.addAll(queueItems.map { song ->
                    val queueView = QueueViewBinder(song, requestManager, sortManager, settingsManager)
                    queueView.setClickListener(songClickListener)
                    queueView.showAlbumArt(true)

                    queueView as ViewModel<*>
                }.toList())

                loadDataDisposable = adapter.setItems(viewModels, object : CompletionListUpdateCallbackAdapter() {
                    override fun onComplete() {
                        updateQueuePosition(position)
                    }
                })
            }
        }
    }

    override fun showToast(message: String, duration: Int) {
        Toast.makeText(context, message, duration).show()
    }

    override fun updateQueuePosition(queuePosition: Int) {

        val queueViewBinders = adapter.items.filterIsInstance<QueueViewBinder>()

        if (queueViewBinders.isEmpty() || queuePosition >= queueViewBinders.size || queuePosition < 0) {
            return
        }

        MultiSheetView.getParentMultiSheetView(view)?.let { multiSheetView ->

            // If we're not currently displaying the queue, then scroll to keep the position up to date
            if (multiSheetView.currentSheet != Sheet.SECOND) {
                if (!queueViewBinders.isEmpty() && queuePosition < queueViewBinders.size) {
                    val index = adapter.items.indexOf(queueViewBinders[queuePosition])
                    if (index >= 0) {
                        recyclerView.scrollToPosition(index)
                    }
                }
            }
        }

        // Deselect previous 'current track'
        queueViewBinders
                .firstOrNull { queueViewBinder -> queueViewBinder.isCurrentTrack }
                ?.let { previouslySelectedQueueViewBinder ->
                    previouslySelectedQueueViewBinder.isCurrentTrack = false
                    adapter.notifyItemChanged(adapter.items.indexOf(previouslySelectedQueueViewBinder), 1)
                }

        // Select the new 'current track'
        queueViewBinders[queuePosition].isCurrentTrack = true
        val index = adapter.items.indexOf(queueViewBinders[queuePosition])
        adapter.notifyItemChanged(index, 1)
    }

    override fun showTaggerDialog(taggerDialog: TaggerDialog) {
        taggerDialog.show(childFragmentManager)
    }

    override fun showDeleteDialog(deleteDialog: DeleteDialog) {
        deleteDialog.show(childFragmentManager)
    }

    override fun onRemovedFromQueue(queueItem: QueueItem) {
        adapter.items.first { viewBinder -> viewBinder is QueueViewBinder && viewBinder.queueItem == queueItem }?.let {
            adapter.removeItem(it)
        }
    }

    override fun onRemovedFromQueue(queueItems: List<QueueItem>) {
        adapter.items.filter { viewBinder -> viewBinder is QueueViewBinder && queueItems.contains(viewBinder.queueItem) }.forEach {
            adapter.removeItem(it)
        }
    }

    override fun showUpgradeDialog() {
        UpgradeDialog().show(childFragmentManager)
    }

    override fun setQueueSwipeLocked(locked: Boolean) {
        itemTouchHelperCallback.setEnabled(!locked)
    }

    override fun showCreatePlaylistDialog(songs: List<Song>) {
        CreatePlaylistDialog.newInstance(mediaManager.queue.toSongs()).show(childFragmentManager, "CreatePlaylistDialog")
    }

    inner class ItemTouchHelperCallback : com.simplecity.amp_library.ui.views.recyclerview.ItemTouchHelperCallback(
            { fromPosition, toPosition -> adapter.moveItem(fromPosition, toPosition) },
            { from, to ->
                val numBeforeFrom = (0..from)
                        .map { value -> adapter.items[value] }
                        .filter { value -> value !is QueueViewBinder }
                        .count()

                val numBeforeTo = (0..to)
                        .map { value -> adapter.items[value] }
                        .filter { value -> value !is QueueViewBinder }
                        .count()

                queuePresenter.moveQueueItem(from - numBeforeFrom, to - numBeforeTo)
            },
            {
                // Nothing to do
            },
            { pos ->
                queuePresenter.removeQueueItem((adapter.items[pos] as QueueViewBinder).queueItem)
            }) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return if (viewHolder.itemViewType == target.itemViewType) {
                super.onMove(recyclerView, viewHolder, target)
            } else false
        }
    }

    class QueueHeaderView(title: String) : SubheaderView(title) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }

        override fun areContentsEqual(other: Any?): Boolean {
            return (other as? QueueHeaderView)?.title == title
        }
    }


    // QueueMenuContract.View Implementation

    override fun presentCreatePlaylistDialog(songs: List<Song>) {
        CreatePlaylistDialog.newInstance(songs).show(childFragmentManager, "CreatePlaylistDialog")
    }

    override fun presentSongInfoDialog(song: Song) {
        SongInfoDialog.newInstance(song).show(childFragmentManager)
    }

    override fun onSongsAddedToPlaylist(playlist: Playlist, numSongs: Int) {
        Toast.makeText(context, context!!.resources.getQuantityString(R.plurals.NNNtrackstoplaylist, numSongs, numSongs), Toast.LENGTH_SHORT).show()
    }

    override fun onSongsAddedToQueue(numSongs: Int) {
        Toast.makeText(context, context!!.resources.getQuantityString(R.plurals.NNNtrackstoqueue, numSongs, numSongs), Toast.LENGTH_SHORT).show()
    }

    override fun presentTagEditorDialog(song: Song) {
        TaggerDialog.newInstance(song).show(childFragmentManager)
    }

    override fun presentDeleteDialog(songs: List<Song>) {
        DeleteDialog.newInstance(DeleteDialog.ListSongsRef { songs }).show(childFragmentManager)
    }

    override fun shareSong(song: Song) {
        song.share(context!!)
    }

    override fun presentRingtonePermissionDialog() {
        RingtoneManager.getDialog(context!!).show()
    }

    override fun showRingtoneSetMessage() {
        Toast.makeText(context, R.string.ringtone_set_new, Toast.LENGTH_SHORT).show()
    }


    // Static

    companion object {

        private const val TAG = "QueueFragment"

        fun newInstance() = QueueFragment()
    }
}
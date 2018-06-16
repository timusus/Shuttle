package com.simplecity.amp_library.ui.queue

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
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.RequestManager
import com.simplecity.amp_library.R
import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.dagger.module.ActivityModule
import com.simplecity.amp_library.dagger.module.FragmentModule
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.playback.MediaManager
import com.simplecity.amp_library.tagger.TaggerDialog
import com.simplecity.amp_library.ui.dialog.DeleteDialog
import com.simplecity.amp_library.ui.dialog.UpgradeDialog
import com.simplecity.amp_library.ui.fragments.BaseFragment
import com.simplecity.amp_library.ui.modelviews.SelectableViewModel
import com.simplecity.amp_library.ui.modelviews.SubheaderView
import com.simplecity.amp_library.ui.presenters.PlayerPresenter
import com.simplecity.amp_library.ui.recyclerview.ItemTouchHelperCallback
import com.simplecity.amp_library.ui.views.ContextualToolbar
import com.simplecity.amp_library.ui.views.PlayerViewAdapter
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetSlideEventRelay
import com.simplecity.amp_library.utils.ContextualToolbarHelper
import com.simplecity.amp_library.utils.ContextualToolbarHelper.Callback
import com.simplecity.amp_library.utils.PermissionUtils
import com.simplecity.amp_library.utils.PlaylistUtils
import com.simplecity.amp_library.utils.ResourceUtils
import com.simplecity.amp_library.utils.ShuttleUtils
import com.simplecity.amp_library.utils.StringUtils
import com.simplecity.amp_library.utils.menu.queue.QueueMenuUtils
import com.simplecity.amp_library.utils.menu.song.SongMenuCallbacksAdapter
import com.simplecity.multisheetview.ui.view.MultiSheetView
import com.simplecity.multisheetview.ui.view.MultiSheetView.Sheet
import com.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter
import com.simplecityapps.recycler_adapter.model.ViewModel
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_queue.contextualToolbar
import kotlinx.android.synthetic.main.fragment_queue.line1
import kotlinx.android.synthetic.main.fragment_queue.line2
import kotlinx.android.synthetic.main.fragment_queue.recyclerView
import kotlinx.android.synthetic.main.fragment_queue.statusBarView
import kotlinx.android.synthetic.main.fragment_queue.toolbar
import java.util.ArrayList
import javax.inject.Inject

class QueueFragment : BaseFragment(), QueueContract.View {

    @Inject
    lateinit var requestManager: RequestManager

    @Inject
    lateinit var multiSheetSlideEventRelay: MultiSheetSlideEventRelay

    @Inject
    lateinit var playerPresenter: PlayerPresenter

    @Inject
    lateinit var queuePresenter: QueuePresenter

    private var loadDataDisposable: Disposable? = null

    private val disposables = CompositeDisposable()

    private lateinit var songMenuCallbacksAdapter: SongMenuCallbacksAdapter

    private var adapter = ViewModelAdapter()

    private lateinit var itemTouchHelper: ItemTouchHelper

    private lateinit var cabToolbar: ContextualToolbar

    private lateinit var cabHelper: ContextualToolbarHelper<QueueItem>

    // Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ShuttleApplication.getInstance().appComponent
            .plus(ActivityModule(activity))
            .plus(FragmentModule(this))
            .inject(this)

        setHasOptionsMenu(true)

        songMenuCallbacksAdapter = object : SongMenuCallbacksAdapter(this, disposables) {

            override fun removeQueueItem(queueItem: QueueItem) {
                queuePresenter.removeFromQueue(queueItem)
            }

            override fun removeQueueItems(queueItems: Single<List<QueueItem>>) {
                queuePresenter.removeFromQueue(queueItems)
            }
        }

        adapter = ViewModelAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_queue, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener { v -> activity?.onBackPressed() }
        toolbar.inflateMenu(R.menu.menu_fragment_queue)

        val subMenu = toolbar.menu.addSubMenu(0, MediaManager.ADD_TO_PLAYLIST, 1, R.string.save_as_playlist)
        disposables.add(PlaylistUtils.createUpdatingPlaylistMenu(subMenu).subscribe())

        toolbar.setOnMenuItemClickListener(toolbarListener)

        cabToolbar = contextualToolbar as ContextualToolbar

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setRecyclerListener(RecyclerListener())
        recyclerView.adapter = adapter

        itemTouchHelper = ItemTouchHelper(object : ItemTouchHelperCallback(
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

                mediaManager.moveQueueItem(from - numBeforeFrom, to - numBeforeTo)
            },
            {
                // Nothing to do
            }) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return if (viewHolder.itemViewType == target.itemViewType) {
                    super.onMove(recyclerView, viewHolder, target)
                } else false
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)

        disposables.add(Aesthetic.get(context)
            .colorPrimary()
            .subscribe { color ->
                val isLight = Util.isColorLight(color!!)
                line1.setTextColor(if (isLight) Color.BLACK else Color.WHITE)
                line2.setTextColor(if (isLight) Color.BLACK else Color.WHITE)
            })

        // In landscape, we need to adjust the status bar's translation depending on the slide offset of the sheet
        if (ShuttleUtils.isLandscape()) {
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
        if (loadDataDisposable != null) {
            loadDataDisposable!!.dispose()
        }
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
                queuePresenter.onQueueItemClick(queueViewBinder.queueItem)
            }
        }

        override fun onQueueItemLongClick(position: Int, queueViewBinder: QueueViewBinder): Boolean {
            return cabHelper.handleLongClick(queueViewBinder, queueViewBinder.queueItem)
        }

        override fun onQueueItemOverflowClick(position: Int, view: View, queueViewBinder: QueueViewBinder) {
            val menu = PopupMenu(view.context, view)
            QueueMenuUtils.setupSongMenu(menu)
            menu.setOnMenuItemClickListener(QueueMenuUtils.getQueueMenuClickListener(queueViewBinder.queueItem, songMenuCallbacksAdapter))
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

        override fun showUpgradeDialog(dialog: MaterialDialog) {
            dialog.show()
        }
    }

    private var toolbarListener = Toolbar.OnMenuItemClickListener { item ->
        when (item.itemId) {
            R.id.menu_clear -> {
                queuePresenter.clearQueue()
                return@OnMenuItemClickListener true
            }
            MediaManager.NEW_PLAYLIST -> {
                queuePresenter.saveQueue(context!!)
                return@OnMenuItemClickListener true
            }
            MediaManager.PLAYLIST_SELECTED -> {
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
        disposables.add(PlaylistUtils.createUpdatingPlaylistMenu(sub).subscribe())
        cabToolbar.setOnMenuItemClickListener(
            QueueMenuUtils.getQueueMenuClickListener(
                Single.fromCallable { cabHelper.items },
                songMenuCallbacksAdapter,
                { cabHelper.finish() }
            )
        )

        cabHelper = ContextualToolbarHelper(cabToolbar, object : Callback {
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
                if (loadDataDisposable != null) {
                    loadDataDisposable!!.dispose()
                }

                val queueHeaderView = QueueHeaderView(
                    StringUtils.makeSongsAndTimeLabel(
                        ShuttleApplication.getInstance(),
                        queueItems.size,
                        queueItems.map { queueItem -> queueItem.song.duration / 1000 }.sum()
                    )
                )

                val viewModels = ArrayList<ViewModel<*>>()
                viewModels.add(queueHeaderView)

                viewModels.addAll(queueItems.map { song ->
                    val queueView = QueueViewBinder(song, requestManager)
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

        if (adapter.items.isEmpty() || queuePosition >= adapter.items.size || queuePosition < 0) {
            return
        }

        val queueViewBinders = adapter.items.filterIsInstance<QueueViewBinder>()

        if (queueViewBinders.isEmpty()) {
            return
        }

        MultiSheetView.getParentMultiSheetView(view)?.let { multiSheetView ->

            // If we're not currently displaying the queue, then scroll to keep the position up to date
            if (multiSheetView.currentSheet != Sheet.SECOND) {
                val index = adapter.items.indexOf(queueViewBinders[queuePosition])
                if (index >= 0) {
                    recyclerView.scrollToPosition(index)
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
        UpgradeDialog.getUpgradeDialog(activity!!).show()
    }

    // Static

    companion object {

        private val TAG = "QueueFragment"

        fun newInstance(): QueueFragment {
            val fragment = QueueFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
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
}

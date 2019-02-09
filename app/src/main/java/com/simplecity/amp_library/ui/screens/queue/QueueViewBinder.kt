package com.simplecity.amp_library.ui.screens.queue

import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.ui.adapters.ViewType
import com.simplecity.amp_library.ui.modelviews.BaseSelectableViewModel
import com.simplecity.amp_library.ui.modelviews.SectionedView
import com.simplecity.amp_library.ui.views.NonScrollImageButton
import com.simplecity.amp_library.utils.PlaceholderProvider
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.StringUtils
import com.simplecity.amp_library.utils.sorting.SortManager
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder

class QueueViewBinder(
    val queueItem: QueueItem,
    private val requestManager: RequestManager,
    private val sortManager: SortManager,
    private val settingsManager: SettingsManager
) :
    BaseSelectableViewModel<QueueViewBinder.ViewHolder>(),
    SectionedView {

    interface ClickListener {

        fun onQueueItemClick(position: Int, queueViewBinder: QueueViewBinder)

        fun onQueueItemLongClick(position: Int, queueViewBinder: QueueViewBinder): Boolean

        fun onQueueItemOverflowClick(position: Int, v: View, queueViewBinder: QueueViewBinder)

        fun onStartDrag(holder: ViewHolder)
    }

    private var showAlbumArt: Boolean = false

    var isCurrentTrack: Boolean = false

    private var listener: ClickListener? = null

    fun setClickListener(listener: ClickListener?) {
        this.listener = listener
    }

    fun showAlbumArt(showAlbumArt: Boolean) {
        this.showAlbumArt = showAlbumArt
    }

    internal fun onItemClick(position: Int) {
        listener?.onQueueItemClick(position, this)
    }

    internal fun onOverflowClick(position: Int, v: View) {
        listener?.onQueueItemOverflowClick(position, v, this)
    }

    internal fun onItemLongClick(position: Int): Boolean {
        return listener?.onQueueItemLongClick(position, this) ?: false
    }

    internal fun onStartDrag(holder: ViewHolder) {
        listener?.onStartDrag(holder)
    }

    override fun getViewType(): Int {
        return ViewType.SONG_EDITABLE
    }

    override fun getLayoutResId(): Int {
        return R.layout.list_item_edit
    }

    override fun bindView(holder: ViewHolder) {
        super.bindView(holder)

        holder.lineOne.text = queueItem.song.name

        holder.lineTwo.text = String.format("%s • %s", queueItem.song.artistName, queueItem.song.albumName)
        holder.lineTwo.visibility = View.VISIBLE

        holder.lineThree.text = queueItem.song.getDurationLabel(holder.itemView.context)

        holder.dragHandle.isActivated = isCurrentTrack

        holder.artwork?.let { artwork ->
            if (showAlbumArt && settingsManager.showArtworkInQueue()) {
                artwork.visibility = View.VISIBLE
                requestManager.load<Song>(queueItem.song)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(PlaceholderProvider.getInstance(holder.itemView.context).getPlaceHolderDrawable(queueItem.song.albumName, false, settingsManager))
                    .into(artwork)
            } else {
                artwork.visibility = View.GONE
            }
        }

        holder.overflowButton.contentDescription = holder.itemView.resources.getString(R.string.btn_options, queueItem.song.name)
    }

    override fun bindView(holder: ViewHolder, position: Int, payloads: List<*>) {
        super.bindView(holder, position, payloads)

        holder.dragHandle.isActivated = isCurrentTrack
    }

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(createView(parent))
    }

    override fun getSectionName(): String {
        val sortOrder = sortManager.songsSortOrder

        if (sortOrder != SortManager.SongSort.DATE
            && sortOrder != SortManager.SongSort.DURATION
            && sortOrder != SortManager.SongSort.TRACK_NUMBER
        ) {

            var string = ""
            var requiresSubstring = true
            when (sortOrder) {
                SortManager.SongSort.DEFAULT -> string = StringUtils.keyFor(queueItem.song.name)
                SortManager.SongSort.NAME -> string = queueItem.song.name
                SortManager.SongSort.YEAR -> {
                    string = queueItem.song.year.toString()
                    if (string.length != 4) {
                        string = "-"
                    } else {
                        string = string.substring(2, 4)
                    }
                    requiresSubstring = false
                }
                SortManager.SongSort.ALBUM_NAME -> string = StringUtils.keyFor(queueItem.song.albumName)
                SortManager.SongSort.ARTIST_NAME -> string = StringUtils.keyFor(queueItem.song.artistName)
            }

            if (requiresSubstring) {
                string = if (!TextUtils.isEmpty(string)) {
                    string.substring(0, 1).toUpperCase()
                } else {
                    ""
                }
            }
            return string
        }
        return ""
    }

    override fun areContentsEqual(other: Any): Boolean {
        return super.areContentsEqual(other) && if (other is QueueViewBinder) {
            queueItem == other.queueItem && isCurrentTrack == other.isCurrentTrack
        } else {
            false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueueViewBinder

        if (queueItem != other.queueItem) return false

        return true
    }

    override fun hashCode(): Int {
        return queueItem.hashCode()
    }

    companion object {
        const val TAG = "QueueViewBinder"
    }

    class ViewHolder constructor(itemView: View) : BaseViewHolder<QueueViewBinder>(itemView) {

        @BindView(R.id.line_one)
        lateinit var lineOne: TextView

        @BindView(R.id.line_two)
        lateinit var lineTwo: TextView

        @BindView(R.id.line_three)
        lateinit var lineThree: TextView

        @BindView(R.id.btn_overflow)
        lateinit var overflowButton: NonScrollImageButton

        @BindView(R.id.drag_handle)
        lateinit var dragHandle: ImageView

        @BindView(R.id.image)
        @JvmField
        var artwork: ImageView? = null

        init {
            ButterKnife.bind(this, itemView)

            itemView.setOnClickListener { v -> viewModel.onItemClick(adapterPosition) }
            itemView.setOnLongClickListener { v -> viewModel.onItemLongClick(adapterPosition) }

            overflowButton.setOnClickListener { v -> viewModel.onOverflowClick(adapterPosition, v) }

            dragHandle?.setOnTouchListener { v, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    viewModel.onStartDrag(this)
                }
                true
            }
        }

        override fun toString(): String {
            return "QueueVeewBinder.ViewHolder"
        }

        override fun recycle() {
            super.recycle()

            artwork?.let { artwork ->
                Glide.clear(artwork)
            }
        }
    }
}
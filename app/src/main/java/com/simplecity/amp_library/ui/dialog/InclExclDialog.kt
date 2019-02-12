package com.simplecity.amp_library.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.simplecity.amp_library.R
import com.simplecity.amp_library.data.Repository
import com.simplecity.amp_library.model.InclExclItem
import com.simplecity.amp_library.ui.modelviews.EmptyView
import com.simplecity.amp_library.ui.modelviews.InclExclView
import com.simplecity.amp_library.utils.AnalyticsManager
import com.simplecity.amp_library.utils.LogUtils
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter
import com.simplecityapps.recycler_adapter.model.ViewModel
import dagger.android.support.AndroidSupportInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class InclExclDialog : DialogFragment() {

    @Inject lateinit var songsRepository: Repository.SongsRepository
    @Inject lateinit var blacklistRepository: Repository.BlacklistRepository
    @Inject lateinit var whitelistRepository: Repository.WhitelistRepository
    @Inject lateinit var analyticsManager: AnalyticsManager

    @InclExclItem.Type
    var type: Int = 0

    private var disposable: Disposable? = null

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        type = arguments!!.getInt(ARG_TYPE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_incl_excl, null)

        val builder = MaterialDialog.Builder(context!!)
            .title(
                when (type) {
                    InclExclItem.Type.INCLUDE -> R.string.whitelist_title
                    else -> R.string.blacklist_title
                }
            )
            .customView(view, false)
            .positiveText(R.string.close)
            .negativeText(R.string.pref_title_clear_whitelist)
            .onNegative { _, _ ->
                when (type) {
                    InclExclItem.Type.INCLUDE -> {
                        whitelistRepository.deleteAll()
                        blacklistRepository.deleteAll()
                    }
                    InclExclItem.Type.EXCLUDE -> blacklistRepository.deleteAll()
                }
                Toast.makeText(
                    context, when (type) {
                        InclExclItem.Type.INCLUDE -> R.string.whitelist_deleted
                        else -> R.string.blacklist_deleted
                    }, Toast.LENGTH_SHORT
                ).show()
            }

        val dialog = builder.build()

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val inclExclAdapter = ViewModelAdapter()

        recyclerView.adapter = inclExclAdapter

        val listener = InclExclView.ClickListener { inclExclView ->
            when (type) {
                InclExclItem.Type.INCLUDE -> {
                    whitelistRepository.delete(inclExclView.inclExclItem)
                    blacklistRepository.delete(inclExclView.inclExclItem)
                }
                InclExclItem.Type.EXCLUDE -> blacklistRepository.delete(inclExclView.inclExclItem)
            }
            if (inclExclAdapter.items.size == 0) {
                dialog.dismiss()
            }
        }

        val items = when (type) {
            InclExclItem.Type.INCLUDE -> whitelistRepository.getWhitelistItems(songsRepository)
            else -> blacklistRepository.getBlacklistItems(songsRepository)
        }

        disposable = items.map<List<ViewModel<*>>> { inclExclItems ->
            inclExclItems
                .map { inclExclItem ->
                    val inclExclView = InclExclView(inclExclItem)
                    inclExclView.setClickListener(listener)
                    inclExclView as ViewModel<*>
                }
                .toList()
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { inclExclViews ->
                    when {
                        inclExclViews.isEmpty() -> {
                            analyticsManager.dropBreadcrumb(TAG, "getDialog setData (empty)")
                            inclExclAdapter.setItems(listOf<ViewModel<*>>(EmptyView(if (type == InclExclItem.Type.INCLUDE) R.string.whitelist_empty else R.string.blacklist_empty)))
                        }
                        else -> {
                            analyticsManager.dropBreadcrumb(TAG, "getDialog setData")
                            inclExclAdapter.setItems(inclExclViews)
                        }
                    }
                },
                { error -> LogUtils.logException(TAG, "Error setting incl/excl items", error) })

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()

        disposable?.dispose()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {

        private const val TAG = "InclExclDialog"

        private const val ARG_TYPE = "type"

        fun newInstance(@InclExclItem.Type type: Int): InclExclDialog {
            val args = Bundle()
            args.putInt(ARG_TYPE, type)
            val fragment = InclExclDialog()
            fragment.arguments = args
            return fragment
        }
    }
}
package com.simplecity.amp_library.ui.settings

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.afollestad.materialdialogs.MaterialDialog
import com.annimon.stream.Stream
import com.simplecity.amp_library.R
import com.simplecity.amp_library.ShuttleApplication
import com.simplecity.amp_library.model.CategoryItem
import com.simplecity.amp_library.ui.dialog.UpgradeDialog
import com.simplecity.amp_library.ui.modelviews.TabViewModel
import com.simplecity.amp_library.ui.screens.main.LibraryController
import com.simplecity.amp_library.ui.views.recyclerview.ItemTouchHelperCallback
import com.simplecity.amp_library.utils.AnalyticsManager
import com.simplecity.amp_library.utils.SettingsManager
import com.simplecity.amp_library.utils.ShuttleUtils
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter
import com.simplecityapps.recycler_adapter.model.ViewModel
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class TabChooserDialog : DialogFragment() {

    @Inject lateinit var analyticsManager: AnalyticsManager

    @Inject lateinit var settingsManager: SettingsManager

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val adapter = ViewModelAdapter()

        val itemTouchHelper = ItemTouchHelper(
            ItemTouchHelperCallback(
                ItemTouchHelperCallback.OnItemMoveListener { fromPosition, toPosition -> adapter.moveItem(fromPosition, toPosition) },
                ItemTouchHelperCallback.OnDropListener { _, _ -> },
                ItemTouchHelperCallback.OnClearListener { },
                ItemTouchHelperCallback.OnSwipeListener { }
            ))

        val listener = object : TabViewModel.Listener {
            override fun onStartDrag(holder: TabViewModel.ViewHolder) {
                itemTouchHelper.startDrag(holder)
            }

            override fun onFolderChecked(tabViewModel: TabViewModel, viewHolder: TabViewModel.ViewHolder) {
                if (!ShuttleUtils.isUpgraded(context!!.applicationContext as ShuttleApplication, settingsManager)) {
                    viewHolder.checkBox.isChecked = false
                    tabViewModel.categoryItem.isChecked = false
                    UpgradeDialog().show(fragmentManager!!)
                }
            }
        }

        val items = CategoryItem.getCategoryItems(sharedPreferences)
            .map { categoryItem ->
                val tabViewModel = TabViewModel(categoryItem, settingsManager)
                tabViewModel.setListener(listener)
                tabViewModel
            }

        analyticsManager.dropBreadcrumb(TAG, "setItems()")
        adapter.setItems(items)

        val recyclerView = RecyclerView(context!!)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        itemTouchHelper.attachToRecyclerView(recyclerView)

        return MaterialDialog.Builder(context!!)
            .title(R.string.pref_title_choose_tabs)
            .customView(recyclerView, false)
            .positiveText(R.string.button_done)
            .onPositive { dialog, which ->
                val editor = sharedPreferences.edit()
                Stream.of<ViewModel<*>>(adapter.items)
                    .indexed()
                    .forEach { viewModelIntPair ->
                        (viewModelIntPair.second as TabViewModel).categoryItem.sortOrder = viewModelIntPair.first
                        (viewModelIntPair.second as TabViewModel).categoryItem.savePrefs(editor)
                    }
                LocalBroadcastManager.getInstance(context!!).sendBroadcast(Intent(LibraryController.EVENT_TABS_CHANGED))
            }
            .negativeText(R.string.close)
            .build()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {

        private const val TAG = "TabChooserDialog"
    }
}
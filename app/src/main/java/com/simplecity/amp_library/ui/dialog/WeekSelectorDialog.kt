package com.simplecity.amp_library.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.widget.NumberPicker
import com.afollestad.materialdialogs.MaterialDialog
import com.simplecity.amp_library.R
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.utils.SettingsManager
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class WeekSelectorDialog : DialogFragment() {

    @Inject lateinit var settingsManager: SettingsManager

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        @SuppressLint("InflateParams")
        val view = LayoutInflater.from(context).inflate(R.layout.weekpicker, null)

        val numberPicker: NumberPicker
        numberPicker = view.findViewById(R.id.weeks)
        numberPicker.maxValue = 12
        numberPicker.minValue = 1
        numberPicker.value = settingsManager.numWeeks

        return MaterialDialog.Builder(context!!)
            .title(R.string.week_selector)
            .customView(view, false)
            .negativeText(R.string.cancel)
            .positiveText(R.string.button_ok)
            .onPositive { _, _ -> settingsManager.numWeeks = numberPicker.value }
            .build()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {

        const val TAG = "WeekSelectorDialog"
    }
}
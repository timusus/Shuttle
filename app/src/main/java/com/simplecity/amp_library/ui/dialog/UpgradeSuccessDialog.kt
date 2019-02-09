package com.simplecity.amp_library.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.simplecity.amp_library.R
import com.simplecity.amp_library.ui.screens.main.MainActivity

class UpgradeSuccessDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context!!)
            .title(context!!.resources.getString(R.string.upgraded_title))
            .content(context!!.resources.getString(R.string.upgraded_message))
            .positiveText(R.string.restart_button)
            .onPositive { _, _ ->
                val intent = Intent(context, MainActivity::class.java)
                val componentName = intent.component
                val mainIntent = Intent.makeRestartActivityTask(componentName)
                startActivity(mainIntent)
            }
            .build()
    }
}
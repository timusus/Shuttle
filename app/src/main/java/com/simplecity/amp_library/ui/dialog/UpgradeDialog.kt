package com.simplecity.amp_library.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.android.billingclient.api.BillingClient
import com.simplecity.amp_library.R
import com.simplecity.amp_library.billing.BillingManager
import com.simplecity.amp_library.constants.Config
import com.simplecity.amp_library.utils.ShuttleUtils
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class UpgradeDialog : DialogFragment() {

    @Inject lateinit var billingManager: BillingManager

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context!!)
            .title(context!!.resources.getString(R.string.get_pro_title))
            .content(context!!.resources.getString(R.string.upgrade_dialog_message))
            .positiveText(R.string.btn_upgrade)
            .onPositive { _, _ ->
                if (ShuttleUtils.isAmazonBuild()) {
                    val storeIntent = ShuttleUtils.getShuttleStoreIntent("com.simplecity.amp_pro")
                    if (storeIntent.resolveActivity(context!!.packageManager) != null) {
                        context!!.startActivity(storeIntent)
                    } else {
                        context!!.startActivity(ShuttleUtils.getShuttleWebIntent("com.simplecity.amp_pro"))
                    }
                } else {
                    billingManager.initiatePurchaseFlow(Config.SKU_PREMIUM, BillingClient.SkuType.INAPP)
                }
            }
            .negativeText(R.string.get_pro_button_no)
            .build()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {
        const val TAG = "UpgradeDialog"

        fun newInstance() = UpgradeDialog()
    }
}
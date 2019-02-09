package com.simplecity.amp_library.ui.views

import android.graphics.Color
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.Snackbar
import android.view.View
import android.widget.TextView
import com.simplecity.amp_library.R
import com.simplecity.amp_library.utils.AnalyticsManager
import com.simplecity.amp_library.utils.SettingsManager

class RatingSnackbar(
    private val settingsManager: SettingsManager,
    private val analyticsManager: AnalyticsManager
) {

    fun show(view: View, onClicked: () -> Unit) {
        //If the user hasn't dismissed the snackbar in the past, and we haven't already shown it for this session
        if (!settingsManager.hasRated && !settingsManager.hasSeenRateSnackbar) {
            //If this is the tenth launch, or a multiple of 50
            if (settingsManager.launchCount == 10 || settingsManager.launchCount != 0 && settingsManager.launchCount % 50 == 0) {
                val snackbar = Snackbar.make(view, R.string.snackbar_rate_text, Snackbar.LENGTH_INDEFINITE)
                    .setDuration(15000)
                    .setAction(R.string.snackbar_rate_action) { v ->
                        onClicked.invoke()
                        analyticsManager.logRateClicked()
                    }
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            super.onDismissed(transientBottomBar, event)

                            if (event != BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_TIMEOUT) {
                                // We don't really care whether the user has rated or not. The snackbar was
                                // dismissed. Never show it again.
                                settingsManager.setHasRated()
                            }
                        }
                    })
                snackbar.show()

                val snackbarText = snackbar.view.findViewById<TextView>(android.support.design.R.id.snackbar_text)
                snackbarText?.setTextColor(Color.WHITE)

                analyticsManager.logRateShown()
            }

            settingsManager.hasSeenRateSnackbar = true
        }
    }
}

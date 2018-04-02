package com.simplecity.amp_library.playback.constants

import android.appwidget.AppWidgetManager
import android.content.Intent

import com.simplecity.amp_library.playback.MusicService
import com.simplecity.amp_library.ui.widgets.WidgetProviderExtraLarge
import com.simplecity.amp_library.ui.widgets.WidgetProviderLarge
import com.simplecity.amp_library.ui.widgets.WidgetProviderMedium
import com.simplecity.amp_library.ui.widgets.WidgetProviderSmall

class WidgetManager {

    private val mWidgetProviderMedium = WidgetProviderMedium.getInstance()
    private val mWidgetProviderSmall = WidgetProviderSmall.getInstance()
    private val mWidgetProviderLarge = WidgetProviderLarge.getInstance()
    private val mWidgetProviderExtraLarge = WidgetProviderExtraLarge.getInstance()

    fun processCommand(musicService: MusicService, intent: Intent, command: String) {
        when (command) {
            WidgetProviderSmall.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                mWidgetProviderSmall.update(musicService, appWidgetIds, true)
            }
            WidgetProviderMedium.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                mWidgetProviderMedium.update(musicService, appWidgetIds, true)
            }
            WidgetProviderLarge.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                mWidgetProviderLarge.update(musicService, appWidgetIds, true)
            }
            WidgetProviderExtraLarge.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                mWidgetProviderExtraLarge.update(musicService, appWidgetIds, true)
            }
        }
    }

    fun notifyChange(musicService: MusicService, what: String) {
        mWidgetProviderLarge.notifyChange(musicService, what)
        mWidgetProviderMedium.notifyChange(musicService, what)
        mWidgetProviderSmall.notifyChange(musicService, what)
        mWidgetProviderExtraLarge.notifyChange(musicService, what)
    }
}
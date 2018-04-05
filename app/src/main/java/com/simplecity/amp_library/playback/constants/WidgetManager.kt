package com.simplecity.amp_library.playback.constants

import android.appwidget.AppWidgetManager
import android.content.Intent

import com.simplecity.amp_library.playback.MusicService
import com.simplecity.amp_library.ui.widgets.WidgetProviderExtraLarge
import com.simplecity.amp_library.ui.widgets.WidgetProviderLarge
import com.simplecity.amp_library.ui.widgets.WidgetProviderMedium
import com.simplecity.amp_library.ui.widgets.WidgetProviderSmall

class WidgetManager {

    private val widgetProviderMedium = WidgetProviderMedium.getInstance()
    private val widgetProviderSmall = WidgetProviderSmall.getInstance()
    private val widgetProviderLarge = WidgetProviderLarge.getInstance()
    private val widgetProviderExtraLarge = WidgetProviderExtraLarge.getInstance()

    fun processCommand(musicService: MusicService, intent: Intent, command: String) {
        when (command) {
            WidgetProviderSmall.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                widgetProviderSmall.update(musicService, appWidgetIds, true)
            }
            WidgetProviderMedium.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                widgetProviderMedium.update(musicService, appWidgetIds, true)
            }
            WidgetProviderLarge.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                widgetProviderLarge.update(musicService, appWidgetIds, true)
            }
            WidgetProviderExtraLarge.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                widgetProviderExtraLarge.update(musicService, appWidgetIds, true)
            }
        }
    }

    fun notifyChange(musicService: MusicService, what: String) {
        widgetProviderLarge.notifyChange(musicService, what)
        widgetProviderMedium.notifyChange(musicService, what)
        widgetProviderSmall.notifyChange(musicService, what)
        widgetProviderExtraLarge.notifyChange(musicService, what)
    }
}
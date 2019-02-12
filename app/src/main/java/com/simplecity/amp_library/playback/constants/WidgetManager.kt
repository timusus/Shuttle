package com.simplecity.amp_library.playback.constants

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.preference.PreferenceManager
import com.simplecity.amp_library.playback.MusicService
import com.simplecity.amp_library.ui.widgets.WidgetProviderExtraLarge
import com.simplecity.amp_library.ui.widgets.WidgetProviderLarge
import com.simplecity.amp_library.ui.widgets.WidgetProviderMedium
import com.simplecity.amp_library.ui.widgets.WidgetProviderSmall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetManager @Inject constructor(
    private val widgetProviderMedium: WidgetProviderMedium,
    private val widgetProviderSmall: WidgetProviderSmall,
    private val widgetProviderLarge: WidgetProviderLarge,
    private val widgetProviderExtraLarge: WidgetProviderExtraLarge
) {

    fun processCommand(musicService: MusicService, intent: Intent, command: String) {

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(musicService)

        when (command) {
            WidgetProviderSmall.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                widgetProviderSmall.update(musicService, sharedPreferences, appWidgetIds, true)
            }
            WidgetProviderMedium.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                widgetProviderMedium.update(musicService, sharedPreferences, appWidgetIds, true)
            }
            WidgetProviderLarge.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                widgetProviderLarge.update(musicService, sharedPreferences, appWidgetIds, true)
            }
            WidgetProviderExtraLarge.CMDAPPWIDGETUPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                widgetProviderExtraLarge.update(musicService, sharedPreferences, appWidgetIds, true)
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
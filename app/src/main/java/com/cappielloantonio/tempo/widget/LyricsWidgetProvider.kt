package com.cappielloantonio.tempo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.ui.activity.MainActivity

class LyricsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    override fun onEnabled(context: Context) {
        LyricsWidgetUpdater.refresh(context)
    }

    override fun onDisabled(context: Context) {
        LyricsWidgetUpdater.cancel(context)
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_lyrics)
            wireClickListeners(context, views)
            LyricsWidgetUpdater.populateState(context, views)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun wireClickListeners(context: Context, views: RemoteViews) {
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPending = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPending)
            views.setOnClickPendingIntent(R.id.widget_album_art, openAppPending)
            views.setOnClickPendingIntent(R.id.widget_title, openAppPending)
            views.setOnClickPendingIntent(R.id.widget_artist, openAppPending)
            views.setOnClickPendingIntent(R.id.widget_lyrics, openAppPending)

            val playPauseIntent = Intent(context, LyricsWidgetActions::class.java).apply {
                action = LyricsWidgetActions.ACTION_PLAY_PAUSE
            }
            val playPausePending = PendingIntent.getBroadcast(
                context, 1, playPauseIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePending)

            val nextIntent = Intent(context, LyricsWidgetActions::class.java).apply {
                action = LyricsWidgetActions.ACTION_NEXT
            }
            val nextPending = PendingIntent.getBroadcast(
                context, 2, nextIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_next, nextPending)
        }

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, LyricsWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            ids.forEach { id ->
                updateAppWidget(context, manager, id)
            }
        }
    }
}

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

class DynamicLyricsWidgetProvider : AppWidgetProvider() {
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
            val views = RemoteViews(context.packageName, R.layout.widget_lyrics_dynamic)
            wireClickListeners(context, views)
            LyricsWidgetUpdater.populateState(context, views)
            LyricsWidgetUpdater.applyDynamicBackground(context, views)
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
            views.setOnClickPendingIntent(R.id.widget_lyrics, openAppPending)

            views.setOnClickPendingIntent(
                R.id.widget_prev,
                broadcastPending(context, LyricsWidgetActions.ACTION_PREVIOUS, 21)
            )
            views.setOnClickPendingIntent(
                R.id.widget_play_pause,
                broadcastPending(context, LyricsWidgetActions.ACTION_PLAY_PAUSE, 22)
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                broadcastPending(context, LyricsWidgetActions.ACTION_NEXT, 23)
            )
            views.setOnClickPendingIntent(
                R.id.widget_shuffle,
                broadcastPending(context, LyricsWidgetActions.ACTION_SHUFFLE, 24)
            )
            views.setOnClickPendingIntent(
                R.id.widget_repeat,
                broadcastPending(context, LyricsWidgetActions.ACTION_REPEAT, 25)
            )

            views.setOnClickPendingIntent(
                R.id.widget_seek_10,
                broadcastPending(context, LyricsWidgetActions.ACTION_SEEK_10, 26)
            )
            views.setOnClickPendingIntent(
                R.id.widget_seek_30,
                broadcastPending(context, LyricsWidgetActions.ACTION_SEEK_30, 27)
            )
            views.setOnClickPendingIntent(
                R.id.widget_seek_50,
                broadcastPending(context, LyricsWidgetActions.ACTION_SEEK_50, 28)
            )
            views.setOnClickPendingIntent(
                R.id.widget_seek_70,
                broadcastPending(context, LyricsWidgetActions.ACTION_SEEK_70, 29)
            )
            views.setOnClickPendingIntent(
                R.id.widget_seek_90,
                broadcastPending(context, LyricsWidgetActions.ACTION_SEEK_90, 30)
            )
        }

        private fun broadcastPending(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, LyricsWidgetActions::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, DynamicLyricsWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            ids.forEach { id ->
                updateAppWidget(context, manager, id)
            }
        }
    }
}

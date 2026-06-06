package com.cappielloantonio.tempo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.Line
import com.cappielloantonio.tempo.subsonic.models.LyricsList
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.Preferences

@OptIn(UnstableApi::class)
object NotificationHelper {
    private data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
    private const val CHANNEL_ID = "tempo_playback_channel"
    private const val CHANNEL_NAME = "Tempo Playback"
    private const val LYRICS_CHANNEL_ID = "tempo_lyrics_channel"
    private const val LYRICS_CHANNEL_NAME = "Tempo Lyrics"
    private const val NOTIFICATION_ID = 1
    private const val LYRICS_NOTIFICATION_ID = 2

    private const val FONT_SIZE_NOTIFICATION_PREV_NEXT_SMALL = 13f
    private const val FONT_SIZE_NOTIFICATION_CURRENT_SMALL = 15f
    private const val FONT_SIZE_NOTIFICATION_PREV_NEXT_MEDIUM = 16f
    private const val FONT_SIZE_NOTIFICATION_CURRENT_MEDIUM = 18f
    private const val FONT_SIZE_NOTIFICATION_PREV_NEXT_LARGE = 20f
    private const val FONT_SIZE_NOTIFICATION_CURRENT_LARGE = 22f

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
            }

            val lyricsChannel = NotificationChannel(
                LYRICS_CHANNEL_ID,
                LYRICS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Song lyrics display"
                setShowBadge(false)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(lyricsChannel)
        }
    }

    fun buildLyricsNotification(
        context: Context,
        title: String?,
        artist: String?,
        prevLine: String?,
        currentLine: String?,
        nextLine1: String?,
        nextLine2: String?,
        isPlaying: Boolean
    ): Notification {
        return Notification.Builder(context, LYRICS_CHANNEL_ID).build()
    }

    fun showLyricsNotification(
        context: Context,
        notificationManager: android.app.NotificationManager,
        title: String?,
        artist: String?,
        prevLine: String?,
        currentLine: String?,
        nextLine1: String?,
        nextLine2: String?,
        isPlaying: Boolean
    ) {
        if (!isPlaying) {
            notificationManager.cancel(LYRICS_NOTIFICATION_ID)
            return
        }

        val lyricsView = RemoteViews(context.packageName, R.layout.notification_small)
        lyricsView.setTextViewText(R.id.notification_lyrics_prev, prevLine ?: "")
        lyricsView.setTextViewText(R.id.notification_lyrics_current, currentLine ?: "")
        lyricsView.setTextViewText(R.id.notification_lyrics_next1, nextLine1 ?: "")
        lyricsView.setTextViewText(R.id.notification_lyrics_next2, nextLine2 ?: "")

        val contentIntent = TaskStackBuilder.create(context).run {
            addNextIntent(Intent(context, MainActivity::class.java))
            getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        lyricsView.setOnClickPendingIntent(R.id.notification_lyrics_current, contentIntent)
        lyricsView.setOnClickPendingIntent(R.id.notification_lyrics_prev, contentIntent)
        lyricsView.setOnClickPendingIntent(R.id.notification_lyrics_next1, contentIntent)
        lyricsView.setOnClickPendingIntent(R.id.notification_lyrics_next2, contentIntent)

        val (currentSp, prevNextSp, showNext2, showPrev) = when (Preferences.getLyricsNotificationFontSize()) {
            1 -> Quadruple(FONT_SIZE_NOTIFICATION_CURRENT_MEDIUM, FONT_SIZE_NOTIFICATION_PREV_NEXT_MEDIUM, false, true)
            2 -> Quadruple(FONT_SIZE_NOTIFICATION_CURRENT_LARGE, FONT_SIZE_NOTIFICATION_PREV_NEXT_LARGE, false, false)
            else -> Quadruple(FONT_SIZE_NOTIFICATION_CURRENT_SMALL, FONT_SIZE_NOTIFICATION_PREV_NEXT_SMALL, true, true)
        }

        lyricsView.setFloat(R.id.notification_lyrics_prev, "setTextSize", prevNextSp)
        lyricsView.setFloat(R.id.notification_lyrics_current, "setTextSize", currentSp)
        lyricsView.setFloat(R.id.notification_lyrics_next1, "setTextSize", prevNextSp)
        lyricsView.setFloat(R.id.notification_lyrics_next2, "setTextSize", prevNextSp)

        lyricsView.setViewVisibility(R.id.notification_lyrics_prev,
            if (showPrev) android.view.View.VISIBLE else android.view.View.GONE)
        lyricsView.setViewVisibility(R.id.notification_lyrics_next2,
            if (showNext2) android.view.View.VISIBLE else android.view.View.GONE)
        lyricsView.setViewVisibility(R.id.notification_lyrics_current, android.view.View.VISIBLE)
        lyricsView.setViewVisibility(R.id.notification_lyrics_next1, android.view.View.VISIBLE)

        val visibility = if (Preferences.isLyricsNotificationLockScreenEnabled()) {
            NotificationCompat.VISIBILITY_PUBLIC
        } else {
            NotificationCompat.VISIBILITY_SECRET
        }

        val notification = NotificationCompat.Builder(context, LYRICS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lyrics)
            .setContentTitle(title ?: "Unknown")
            .setContentText(currentLine ?: "")
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(visibility)
            .setCustomContentView(lyricsView)
            .setOngoing(true)
            .build()

        notificationManager.notify(LYRICS_NOTIFICATION_ID, notification)
    }

    fun cancelLyricsNotification(notificationManager: android.app.NotificationManager) {
        notificationManager.cancel(LYRICS_NOTIFICATION_ID)
    }

    fun getLyricsNotificationId(): Int = LYRICS_NOTIFICATION_ID

    private fun getCurrentLyricsText(lyricsList: LyricsList?, position: Long): String {
        val structuredLyrics = lyricsList?.structuredLyrics
        if (structuredLyrics.isNullOrEmpty()) {
            return "No lyrics available"
        }

        val lines = structuredLyrics[0].line
        if (lines.isNullOrEmpty()) {
            return "No lyrics available"
        }

        var currentLine: Line? = null
        for (line in lines) {
            val startTime = line.start
            if (startTime != null && startTime <= position) {
                currentLine = line
            } else {
                break
            }
        }

        return currentLine?.value?.trim() ?: "No lyrics available"
    }
}

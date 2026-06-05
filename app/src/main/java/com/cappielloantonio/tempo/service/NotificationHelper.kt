package com.cappielloantonio.tempo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.subsonic.models.Line
import com.cappielloantonio.tempo.subsonic.models.LyricsList
import com.cappielloantonio.tempo.ui.activity.MainActivity

@OptIn(UnstableApi::class)
object NotificationHelper {
    private const val CHANNEL_ID = "tempo_playback_channel"
    private const val CHANNEL_NAME = "Tempo Playback"
    private const val LYRICS_CHANNEL_ID = "tempo_lyrics_channel"
    private const val LYRICS_CHANNEL_NAME = "Tempo Lyrics"
    private const val NOTIFICATION_ID = 1
    private const val LYRICS_NOTIFICATION_ID = 2

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

        val visibility = if (com.cappielloantonio.tempo.util.Preferences.isLyricsNotificationLockScreenEnabled()) {
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
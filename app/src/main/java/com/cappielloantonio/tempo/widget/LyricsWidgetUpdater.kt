package com.cappielloantonio.tempo.widget

import android.content.Context
import android.widget.RemoteViews
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.service.MediaService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object LyricsWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var refreshJob: Job? = null

    private var lastStateHash: Int = -1
    private var lastArtworkUrl: String? = null
    private var lastBitmap: android.graphics.Bitmap? = null

    fun refresh(context: Context) {
        LyricsWidgetProvider.refreshAll(context.applicationContext)
        startPeriodicRefresh(context.applicationContext)
    }

    fun cancel(context: Context) {
        refreshJob?.cancel()
        refreshJob = null
        scope.cancel()
    }

    private fun startPeriodicRefresh(context: Context) {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            while (isActive) {
                LyricsWidgetProvider.refreshAll(context)
                delay(1000)
            }
        }
    }

    fun populateState(context: Context, views: RemoteViews) {
        val mediaItem = MediaService.getCurrentMediaItem()
        val isPlaying = MediaService.isPlaying()
        val title = mediaItem?.mediaMetadata?.title?.toString() ?: context.getString(R.string.widget_no_track)
        val artist = mediaItem?.mediaMetadata?.artist?.toString() ?: context.getString(R.string.widget_no_artist)
        val lyrics = MediaService.getLyricsAtPosition(MediaService.getCurrentPosition()).current

        views.setTextViewText(R.id.widget_title, title)
        views.setTextViewText(R.id.widget_artist, artist)
        views.setTextViewText(R.id.widget_lyrics, lyrics.ifEmpty { context.getString(R.string.widget_no_lyrics) })
        views.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )

        if (lastBitmap != null && lastArtworkUrl == mediaItem?.mediaMetadata?.artworkUri?.toString()) {
            views.setImageViewBitmap(R.id.widget_album_art, lastBitmap)
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_placeholder_album)
            val url = mediaItem?.mediaMetadata?.artworkUri?.toString()
            if (url != null && url != lastArtworkUrl) {
                lastArtworkUrl = url
                loadAlbumArt(context, url)
            }
        }
    }

    private fun loadAlbumArt(context: Context, url: String) {
        val target = object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
            override fun onResourceReady(
                resource: android.graphics.Bitmap,
                transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?
            ) {
                lastBitmap = resource
                LyricsWidgetProvider.refreshAll(context)
            }

            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
            }
        }
        com.bumptech.glide.Glide.with(context.applicationContext)
            .asBitmap()
            .load(url)
            .into(target)
    }
}

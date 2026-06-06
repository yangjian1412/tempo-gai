package com.cappielloantonio.tempo.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
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

    private var lastArtworkUrl: String? = null
    private var lastBitmap: android.graphics.Bitmap? = null
    private var lastProcessedBackground: android.graphics.Bitmap? = null
    private var lastProcessedSignature: Pair<String, Boolean>? = null

    fun refresh(context: Context) {
        LyricsWidgetProvider.refreshAll(context.applicationContext)
        SolidLyricsWidgetProvider.refreshAll(context.applicationContext)
        DynamicLyricsWidgetProvider.refreshAll(context.applicationContext)
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
                SolidLyricsWidgetProvider.refreshAll(context)
                DynamicLyricsWidgetProvider.refreshAll(context)
                delay(1000)
            }
        }
    }

    fun populateState(context: Context, views: RemoteViews) {
        val mediaItem = MediaService.getCurrentMediaItem()
        val isPlaying = MediaService.isPlaying()

        val extras = mediaItem?.mediaMetadata?.extras
        val originalTitle = extras?.getString("original_title")
            ?.takeIf { it.isNotBlank() }
            ?: mediaItem?.mediaMetadata?.title?.toString()
        val originalArtist = extras?.getString("original_artist")
            ?.takeIf { it.isNotBlank() }
            ?: mediaItem?.mediaMetadata?.artist?.toString()

        val titleText = originalTitle ?: context.getString(R.string.widget_no_track)
        val artistText = originalArtist.orEmpty()
        val titleWithArtist = if (artistText.isNotEmpty()) "$titleText - $artistText" else titleText

        val lyrics = MediaService.getLyricsAtPosition(MediaService.getCurrentPosition()).current

        views.setTextViewText(R.id.widget_title, titleWithArtist)
        views.setTextViewText(
            R.id.widget_lyrics,
            lyrics.ifEmpty { context.getString(R.string.widget_no_lyrics) }
        )
        views.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )

        views.setImageViewResource(
            R.id.widget_shuffle,
            if (MediaService.isShuffleEnabled()) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle
        )
        views.setImageViewResource(
            R.id.widget_repeat,
            when (MediaService.getRepeatMode()) {
                Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
                else -> R.drawable.ic_repeat
            }
        )

        val position = MediaService.getCurrentPosition()
        val duration = MediaService.getDuration()
        val progress = if (duration > 0) {
            ((position.toDouble() / duration.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0
        views.setProgressBar(R.id.widget_progress, 100, progress, false)

        if (lastBitmap != null && lastArtworkUrl == mediaItem?.mediaMetadata?.artworkUri?.toString()) {
            views.setImageViewBitmap(R.id.widget_album_art, lastBitmap)
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_placeholder_album)
            val url = mediaItem?.mediaMetadata?.artworkUri?.toString()
            if (url != null && url != lastArtworkUrl) {
                lastArtworkUrl = url
                lastProcessedBackground = null
                loadAlbumArt(context, url)
            }
        }
    }

    fun applyDynamicBackground(context: Context, views: RemoteViews) {
        val isNight = isNightMode(context)
        val primaryText = ContextCompat.getColor(context, R.color.widget_text_primary)
        val secondaryText = ContextCompat.getColor(context, R.color.widget_text_secondary)
        views.setInt(R.id.widget_title, "setTextColor", primaryText)
        views.setInt(R.id.widget_lyrics, "setTextColor", secondaryText)

        val source = lastBitmap
        if (source == null) {
            views.setImageViewResource(
                R.id.widget_dynamic_background,
                R.drawable.widget_album_background_solid
            )
            lastProcessedBackground = null
            lastProcessedSignature = null
            return
        }

        val signature = (lastArtworkUrl ?: "unknown") to isNight
        val processed = if (lastProcessedSignature == signature && lastProcessedBackground != null) {
            lastProcessedBackground
        } else {
            val created = processAlbumBackground(source, isNight)
            lastProcessedBackground = created
            lastProcessedSignature = signature
            created
        }
        views.setImageViewBitmap(R.id.widget_dynamic_background, processed)
    }

    private fun isNightMode(context: Context): Boolean {
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun processAlbumBackground(source: Bitmap, isNight: Boolean): Bitmap {
        val targetW = 1200
        val targetH = 480
        val blurScale = 12
        val blurRadius = 4

        val smallW = (source.width / blurScale).coerceAtLeast(1)
        val smallH = (source.height / blurScale).coerceAtLeast(1)
        var small = Bitmap.createScaledBitmap(source, smallW, smallH, true)
        val pass1 = boxBlur(small, blurRadius)
        if (pass1 != small) small.recycle()
        val pass2 = boxBlur(pass1, blurRadius)
        if (pass2 != pass1) pass1.recycle()
        val blurred = Bitmap.createScaledBitmap(pass2, source.width, source.height, true)
        if (blurred != pass2) pass2.recycle()

        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val srcRect = centerCropRect(blurred.width, blurred.height, targetW, targetH)
        val dstRect = Rect(0, 0, targetW, targetH)
        canvas.drawBitmap(blurred, srcRect, dstRect, null)
        blurred.recycle()

        val maskColor = if (isNight) 0x40000000.toInt() else 0x40FFFFFF.toInt()
        val maskPaint = Paint().apply {
            color = maskColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, targetW.toFloat(), targetH.toFloat(), maskPaint)
        return output
    }

    private fun boxBlur(source: Bitmap, radius: Int): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val temp = IntArray(w * h)
        val output = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                var a = 0; var r = 0; var g = 0; var b = 0
                var count = 0
                val xStart = maxOf(0, x - radius)
                val xEnd = minOf(w - 1, x + radius)
                for (xx in xStart..xEnd) {
                    val p = pixels[y * w + xx]
                    a += (p ushr 24) and 0xff
                    r += (p ushr 16) and 0xff
                    g += (p ushr 8) and 0xff
                    b += p and 0xff
                    count++
                }
                temp[y * w + x] = ((a / count) shl 24) or
                    ((r / count) shl 16) or
                    ((g / count) shl 8) or
                    (b / count)
            }
        }

        for (y in 0 until h) {
            for (x in 0 until w) {
                var a = 0; var r = 0; var g = 0; var b = 0
                var count = 0
                val yStart = maxOf(0, y - radius)
                val yEnd = minOf(h - 1, y + radius)
                for (yy in yStart..yEnd) {
                    val p = temp[yy * w + x]
                    a += (p ushr 24) and 0xff
                    r += (p ushr 16) and 0xff
                    g += (p ushr 8) and 0xff
                    b += p and 0xff
                    count++
                }
                output[y * w + x] = ((a / count) shl 24) or
                    ((r / count) shl 16) or
                    ((g / count) shl 8) or
                    (b / count)
            }
        }

        return Bitmap.createBitmap(output, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun centerCropRect(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Rect {
        val srcRatio = srcW.toFloat() / srcH.toFloat()
        val dstRatio = dstW.toFloat() / dstH.toFloat()
        return if (srcRatio > dstRatio) {
            val newW = (srcH * dstRatio).toInt()
            val xOff = (srcW - newW) / 2
            Rect(xOff, 0, xOff + newW, srcH)
        } else {
            val newH = (srcW / dstRatio).toInt()
            val yOff = (srcH - newH) / 2
            Rect(0, yOff, srcW, yOff + newH)
        }
    }

    private fun loadAlbumArt(context: Context, url: String) {
        val target = object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
            override fun onResourceReady(
                resource: android.graphics.Bitmap,
                transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?
            ) {
                lastBitmap = resource
                lastProcessedBackground = null
                LyricsWidgetProvider.refreshAll(context)
                SolidLyricsWidgetProvider.refreshAll(context)
                DynamicLyricsWidgetProvider.refreshAll(context)
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

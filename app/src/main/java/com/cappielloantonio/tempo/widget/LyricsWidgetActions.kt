package com.cappielloantonio.tempo.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cappielloantonio.tempo.service.MediaService

class LyricsWidgetActions : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                if (MediaService.isPlaying()) {
                    MediaService.pause()
                } else {
                    MediaService.play()
                }
            }
            ACTION_NEXT -> MediaService.seekToNext()
            ACTION_PREVIOUS -> MediaService.seekToPrevious()
            ACTION_SHUFFLE -> MediaService.toggleShuffle()
            ACTION_REPEAT -> MediaService.toggleRepeat()
            ACTION_SEEK_10 -> seekToFraction(0.1f)
            ACTION_SEEK_30 -> seekToFraction(0.3f)
            ACTION_SEEK_50 -> seekToFraction(0.5f)
            ACTION_SEEK_70 -> seekToFraction(0.7f)
            ACTION_SEEK_90 -> seekToFraction(0.9f)
        }
    }

    private fun seekToFraction(fraction: Float) {
        val duration = MediaService.getDuration()
        if (duration > 0) {
            MediaService.seekToPosition((duration * fraction).toLong())
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.cappielloantonio.tempo.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.cappielloantonio.tempo.widget.NEXT"
        const val ACTION_PREVIOUS = "com.cappielloantonio.tempo.widget.PREVIOUS"
        const val ACTION_SHUFFLE = "com.cappielloantonio.tempo.widget.SHUFFLE"
        const val ACTION_REPEAT = "com.cappielloantonio.tempo.widget.REPEAT"
        const val ACTION_SEEK_10 = "com.cappielloantonio.tempo.widget.SEEK_10"
        const val ACTION_SEEK_30 = "com.cappielloantonio.tempo.widget.SEEK_30"
        const val ACTION_SEEK_50 = "com.cappielloantonio.tempo.widget.SEEK_50"
        const val ACTION_SEEK_70 = "com.cappielloantonio.tempo.widget.SEEK_70"
        const val ACTION_SEEK_90 = "com.cappielloantonio.tempo.widget.SEEK_90"
    }
}

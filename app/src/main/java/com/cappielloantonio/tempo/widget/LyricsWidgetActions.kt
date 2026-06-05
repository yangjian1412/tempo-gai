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
            ACTION_NEXT -> {
                MediaService.seekToNext()
            }
            ACTION_PREVIOUS -> {
                MediaService.seekToPrevious()
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.cappielloantonio.tempo.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.cappielloantonio.tempo.widget.NEXT"
        const val ACTION_PREVIOUS = "com.cappielloantonio.tempo.widget.PREVIOUS"
    }
}

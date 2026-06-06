package com.cappielloantonio.tempo.service

import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession.ControllerInfo
import com.cappielloantonio.tempo.repository.AutomotiveRepository
import com.cappielloantonio.tempo.repository.OpenRepository
import com.cappielloantonio.tempo.repository.OpenRepository.LyricsCallback
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.LyricsList
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.ReplayGainUtil
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

@UnstableApi
class MediaService : MediaLibraryService(), SessionAvailabilityListener {
    private lateinit var automotiveRepository: AutomotiveRepository
    private lateinit var openRepository: OpenRepository
    private lateinit var player: ExoPlayer
    private lateinit var castPlayer: CastPlayer
    private lateinit var mediaLibrarySession: MediaLibrarySession

    private var currentLyrics: String? = null
    private var currentLyricsList: LyricsList? = null
    private val handler = Handler(Looper.getMainLooper())
    private var updateLyricsNotificationRunnable: Runnable? = null
    private var lastInjectedLyricLine: String? = null
    private var lastInjectedSongId: String? = null

    companion object {
        private var instance: MediaService? = null

        fun getInstance(): MediaService? = instance

        @JvmStatic
        fun updateLyrics(lyrics: String?, lyricsList: LyricsList?) {
            instance?.apply {
                currentLyrics = lyrics
                currentLyricsList = lyricsList
            }
        }

        @JvmStatic
        fun isPlaying(): Boolean = instance?.player?.isPlaying == true

        @JvmStatic
        fun play() {
            instance?.player?.play()
        }

        @JvmStatic
        fun pause() {
            instance?.player?.pause()
        }

        @JvmStatic
        fun seekToPrevious() {
            instance?.player?.seekToPrevious()
        }

        @JvmStatic
        fun seekToNext() {
            instance?.player?.seekToNext()
        }

        @JvmStatic
        fun seekToPosition(position: Long) {
            val player = instance?.player ?: return
            val duration = player.duration
            if (duration > 0) {
                player.seekTo(position.coerceIn(0L, duration))
            }
        }

        @JvmStatic
        fun getCurrentPosition(): Long = instance?.player?.currentPosition ?: 0L

        @JvmStatic
        fun getCurrentMediaItem(): MediaItem? = instance?.player?.currentMediaItem

        @JvmStatic
        fun getLyricsAtPosition(position: Long): LyricsQuadruple {
            return instance?.getLyricsLines(position) ?: LyricsQuadruple("", "", "", "")
        }

        @JvmStatic
        fun getDuration(): Long = instance?.player?.duration ?: 0L

        @JvmStatic
        fun isShuffleEnabled(): Boolean = instance?.player?.shuffleModeEnabled == true

        @JvmStatic
        fun toggleShuffle() {
            instance?.player?.let { player ->
                player.shuffleModeEnabled = !player.shuffleModeEnabled
            }
        }

        @JvmStatic
        fun getRepeatMode(): Int = instance?.player?.repeatMode ?: 0

        @JvmStatic
        fun toggleRepeat() {
            instance?.player?.let { player ->
                player.repeatMode = when (player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        NotificationHelper.createNotificationChannel(this)

        initializeRepository()
        initializePlayer()
        initializeCastPlayer()
        initializeMediaLibrarySession()
        initializePlayerListener()
        startLyricsNotificationLoop()

        setPlayer(
                null,
                if (this::castPlayer.isInitialized && castPlayer.isCastSessionAvailable) castPlayer else player
        )
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaLibrarySession {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession.player

        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopLyricsNotificationLoop()
        releasePlayer()
        super.onDestroy()
    }

    private fun startLyricsNotificationLoop() {
        updateLyricsNotificationRunnable = object : Runnable {
            override fun run() {
                updateLyricsNotification()
                handler.postDelayed(this, 100)
            }
        }
        handler.post(updateLyricsNotificationRunnable!!)
    }

    private fun stopLyricsNotificationLoop() {
        updateLyricsNotificationRunnable?.let { handler.removeCallbacks(it) }
        updateLyricsNotificationRunnable = null
    }

    private fun updateLyricsNotification() {
        if (!::player.isInitialized) return

        val mediaItem = player.currentMediaItem ?: return
        val isPlaying = player.isPlaying
        val position = player.currentPosition

        val title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown"
        val artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist"

        val hasLyrics = currentLyricsList?.structuredLyrics?.isNotEmpty() == true

        val (prevLine, currentLine, nextLine1, nextLine2) = if (hasLyrics) {
            getLyricsLines(position)
        } else {
            LyricsQuadruple("", "", "", "")
        }

        if (Preferences.isLyricsNotificationEnabled()) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            NotificationHelper.showLyricsNotification(
                this,
                notificationManager,
                if (hasLyrics) "" else title,
                if (hasLyrics) "" else artist,
                prevLine,
                currentLine,
                nextLine1,
                nextLine2,
                isPlaying
            )
        } else {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            NotificationHelper.cancelLyricsNotification(notificationManager)
        }

        if (Preferences.isDesktopLyricsEnabled() && hasLyrics) {
            DesktopLyricsOverlay.show(this, prevLine, currentLine, nextLine1, nextLine2)
        } else {
            DesktopLyricsOverlay.hide()
        }

        if (Preferences.isSystemPlayerLyricsEnabled() && hasLyrics) {
            injectLyricsIntoMediaSession(currentLine, nextLine1, mediaItem.mediaId)
        } else {
            restoreOriginalMetadata(mediaItem.mediaId)
        }

        com.cappielloantonio.tempo.widget.LyricsWidgetUpdater.refresh(this)
    }

    private fun restoreOriginalMetadata(mediaId: String?) {
        if (lastInjectedLyricLine == null) return

        val currentMediaItem = player.currentMediaItem ?: return
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex < 0) return

        val extras = currentMediaItem.mediaMetadata.extras
        val originalTitle = extras?.getString("original_title")
        val originalArtist = extras?.getString("original_artist")

        if (originalTitle.isNullOrEmpty()) {
            lastInjectedLyricLine = null
            lastInjectedSongId = null
            return
        }

        val newMetadata = currentMediaItem.mediaMetadata.buildUpon().apply {
            setTitle(originalTitle)
            if (!originalArtist.isNullOrEmpty()) {
                setArtist(originalArtist)
            }
        }.build()

        val newItem = currentMediaItem.buildUpon()
            .setMediaMetadata(newMetadata)
            .build()

        try {
            player.replaceMediaItem(currentIndex, newItem)
        } catch (_: Exception) {
        }
        lastInjectedLyricLine = null
        lastInjectedSongId = null
    }

    private fun injectLyricsIntoMediaSession(currentLine: String, nextLine: String, mediaId: String?) {
        if (currentLine == lastInjectedLyricLine && mediaId == lastInjectedSongId) return

        val currentMediaItem = player.currentMediaItem ?: return
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex < 0) return

        val oldExtras = currentMediaItem.mediaMetadata.extras
        val newExtras = Bundle()
        if (oldExtras != null) {
            newExtras.putAll(oldExtras)
        }

        val originalTitle = newExtras.getString("original_title")
            ?: currentMediaItem.mediaMetadata.title?.toString().orEmpty()
        val originalArtist = newExtras.getString("original_artist")
            ?: currentMediaItem.mediaMetadata.artist?.toString().orEmpty()

        newExtras.putString("original_title", originalTitle)
        newExtras.putString("original_artist", originalArtist)
        newExtras.putString("current_lyric", currentLine)
        newExtras.putString("next_lyric", nextLine)
        newExtras.putString("lyric_time", player.currentPosition.toString())

        val newMetadataBuilder = currentMediaItem.mediaMetadata.buildUpon()
            .setTitle(currentLine)
            .setExtras(newExtras)

        if (originalArtist.isNotEmpty() && originalTitle.isNotEmpty()) {
            newMetadataBuilder.setArtist("$originalArtist - $originalTitle")
        } else if (originalArtist.isNotEmpty()) {
            newMetadataBuilder.setArtist(originalArtist)
        }

        val newMetadata = newMetadataBuilder.build()

        val newItem = currentMediaItem.buildUpon()
            .setMediaMetadata(newMetadata)
            .build()

        try {
            player.replaceMediaItem(currentIndex, newItem)
            lastInjectedLyricLine = currentLine
            lastInjectedSongId = mediaId
        } catch (_: Exception) {
        }
    }

    private fun getLyricsLines(position: Long): LyricsQuadruple {
        val lyricsList = currentLyricsList ?: return LyricsQuadruple("", "", "", "")

        val structuredLyrics = lyricsList.structuredLyrics
        if (structuredLyrics.isNullOrEmpty()) return LyricsQuadruple("", "", "", "")

        val lines = structuredLyrics[0].line
        if (lines.isNullOrEmpty()) return LyricsQuadruple("", "", "", "")

        var currentIndex = -1
        for (i in lines.indices) {
            val startTime = lines[i].start
            if (startTime != null && startTime <= position) {
                currentIndex = i
            } else {
                break
            }
        }

        val prevLine = if (currentIndex > 0) lines[currentIndex - 1].value?.trim() ?: "" else ""
        val currentLine = if (currentIndex >= 0) lines[currentIndex].value?.trim() ?: "" else ""
        val nextLine1 = if (currentIndex + 1 < lines.size) lines[currentIndex + 1].value?.trim() ?: "" else ""
        val nextLine2 = if (currentIndex + 2 < lines.size) lines[currentIndex + 2].value?.trim() ?: "" else ""

        return LyricsQuadruple(prevLine, currentLine, nextLine1, nextLine2)
    }

    data class LyricsQuadruple(val prev: String, val current: String, val next1: String, val next2: String)

    private fun initializeRepository() {
        automotiveRepository = AutomotiveRepository()
        openRepository = OpenRepository()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
                .setRenderersFactory(getRenderersFactory())
                .setMediaSourceFactory(getMediaSourceFactory())
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setLoadControl(initializeLoadControl())
                .build()
    }

    private fun initializeCastPlayer() {
        if (GoogleApiAvailability.getInstance()
                        .isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
        ) {
            castPlayer = CastPlayer(CastContext.getSharedInstance(this))
            castPlayer.setSessionAvailabilityListener(this)
        }
    }

    private fun initializeMediaLibrarySession() {
        val sessionActivityPendingIntent =
                TaskStackBuilder.create(this).run {
                    addNextIntent(Intent(this@MediaService, MainActivity::class.java))
                    getPendingIntent(0, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
                }

        mediaLibrarySession =
                MediaLibrarySession.Builder(this, player, createLibrarySessionCallback())
                        .setSessionActivity(sessionActivityPendingIntent)
                        .build()
    }

    private fun createLibrarySessionCallback(): MediaLibrarySession.Callback {
        return MediaLibrarySessionCallback(this, automotiveRepository)
    }

    private fun initializePlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem == null) return

                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    return
                }

                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK || reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    MediaManager.setLastPlayedTimestamp(mediaItem)
                }

                currentLyrics = null
                currentLyricsList = null
                lastInjectedLyricLine = null
                lastInjectedSongId = null

                val songId = mediaItem.mediaMetadata.extras?.getString("id")
                if (songId != null) {
                    openRepository.getLyricsBySongId(songId, object : OpenRepository.LyricsCallback {
                        override fun onSuccess(lyricsList: LyricsList) {
                            currentLyricsList = lyricsList
                        }

                        override fun onFailure() {
                        }
                    })
                }

                val notificationManager = this@MediaService.getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                val title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown"
                val artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist"
                NotificationHelper.showLyricsNotification(
                    this@MediaService,
                    notificationManager,
                    title,
                    artist,
                    "", "", "", "",
                    player.isPlaying
                )
            }

            override fun onTracksChanged(tracks: Tracks) {
                ReplayGainUtil.setReplayGain(player, tracks)
                MediaManager.scrobble(player.currentMediaItem, false)

                if (player.currentMediaItemIndex + 1 == player.mediaItemCount)
                    MediaManager.continuousPlay(player.currentMediaItem)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) {
                    MediaManager.setPlayingPausedTimestamp(
                            player.currentMediaItem,
                            player.currentPosition
                    )
                } else {
                    MediaManager.scrobble(player.currentMediaItem, false)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)

                if (!player.hasNextMediaItem() &&
                        playbackState == Player.STATE_ENDED &&
                        player.mediaMetadata.extras?.getString("type") == Constants.MEDIA_TYPE_MUSIC
                ) {
                    MediaManager.scrobble(player.currentMediaItem, true)
                    MediaManager.saveChronology(player.currentMediaItem)
                }
            }

            override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
            ) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)

                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    if (oldPosition.mediaItem?.mediaMetadata?.extras?.getString("type") == Constants.MEDIA_TYPE_MUSIC) {
                        MediaManager.scrobble(oldPosition.mediaItem, true)
                        MediaManager.saveChronology(oldPosition.mediaItem)
                    }

                    if (newPosition.mediaItem?.mediaMetadata?.extras?.getString("type") == Constants.MEDIA_TYPE_MUSIC) {
                        MediaManager.setLastPlayedTimestamp(newPosition.mediaItem)
                    }
                }
            }
        })
    }

    private fun initializeLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        (DefaultLoadControl.DEFAULT_MIN_BUFFER_MS * Preferences.getBufferingStrategy()).toInt(),
                        (DefaultLoadControl.DEFAULT_MAX_BUFFER_MS * Preferences.getBufferingStrategy()).toInt(),
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                .build()
    }

    private fun setPlayer(oldPlayer: Player?, newPlayer: Player) {
        if (oldPlayer === newPlayer) return
        oldPlayer?.stop()
        mediaLibrarySession.player = newPlayer
    }

    private fun releasePlayer() {
        if (this::castPlayer.isInitialized) castPlayer.setSessionAvailabilityListener(null)
        if (this::castPlayer.isInitialized) castPlayer.release()
        player.release()
        mediaLibrarySession.release()
        automotiveRepository.deleteMetadata()
        clearListener()
    }

    private fun getRenderersFactory() = DownloadUtil.buildRenderersFactory(this, false)

    private fun getMediaSourceFactory() =
            DefaultMediaSourceFactory(this).setDataSourceFactory(DownloadUtil.getDataSourceFactory(this))

    override fun onCastSessionAvailable() {
        setPlayer(player, castPlayer)
    }

    override fun onCastSessionUnavailable() {
        setPlayer(castPlayer, player)
    }
}
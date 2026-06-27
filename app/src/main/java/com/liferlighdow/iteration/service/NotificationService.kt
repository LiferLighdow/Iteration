package com.liferlighdow.iteration.service

import android.app.Notification
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaInfo(
    val title: String? = null,
    val artist: String? = null,
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val packageName: String? = null
)

class NotificationService : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<Map<String, Int>>(emptyMap())
        val notifications = _notifications.asStateFlow()

        private val _currentMedia = MutableStateFlow<MediaInfo?>(null)
        val currentMedia = _currentMedia.asStateFlow()

        private var activeController: MediaController? = null

        fun sendMediaCommand(command: String) {
            val controller = activeController ?: return
            when (command) {
                "play_pause" -> {
                    val state = controller.playbackState?.state
                    if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                        controller.transportControls.pause()
                    } else {
                        controller.transportControls.play()
                    }
                }
                "next" -> controller.transportControls.skipToNext()
                "previous" -> controller.transportControls.skipToPrevious()
            }
        }
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaInfo()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaInfo()
        }

        override fun onSessionDestroyed() {
            activeController?.unregisterCallback(this)
            activeController = null
            findActiveMediaSession()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateCounts()
        checkMediaNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateCounts()
        if (sbn != null && sbn.packageName == activeController?.packageName) {
            val stillHasMedia = try {
                activeNotifications?.any {
                    it.packageName == sbn.packageName && it.notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
                } ?: false
            } catch (e: SecurityException) {
                false
            }

            if (!stillHasMedia) {
                activeController?.unregisterCallback(controllerCallback)
                activeController = null
                findActiveMediaSession()
            }
        }
    }

    override fun onListenerConnected() {
        updateCounts()
        findActiveMediaSession()
    }

    private fun updateCounts() {
        val activeNotifs = try {
            activeNotifications
        } catch (e: SecurityException) {
            null
        } ?: return

        val counts = activeNotifs
            .filter { !it.isOngoing && it.packageName != packageName }
            .groupBy { it.packageName }
            .mapValues { it.value.size }
        _notifications.value = counts
    }

    private fun checkMediaNotification(sbn: StatusBarNotification?) {
        val token = sbn?.notification?.extras?.get(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token
        if (token != null) {
            val currentIsPlaying = activeController?.playbackState?.state == PlaybackState.STATE_PLAYING
            val newController = MediaController(this, token)
            val newIsPlaying = newController.playbackState?.state == PlaybackState.STATE_PLAYING

            // 優先保留正在播放的 Session，或是如果是同一個 App 的更新則替換
            if (activeController == null || newIsPlaying || (activeController?.packageName == sbn.packageName) || !currentIsPlaying) {
                setupMediaController(token)
            }
        } else if (sbn?.packageName == activeController?.packageName) {
            updateMediaInfo()
        }
    }

    private fun findActiveMediaSession() {
        val activeNotifs = try {
            activeNotifications
        } catch (e: SecurityException) {
            null
        } ?: return

        var foundToken: MediaSession.Token? = null

        for (sbn in activeNotifs) {
            val token = sbn.notification?.extras?.get(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token
            if (token != null) {
                val controller = MediaController(this, token)
                if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    setupMediaController(token)
                    return
                }
                if (foundToken == null) {
                    foundToken = token
                }
            }
        }

        if (foundToken != null) {
            setupMediaController(foundToken)
        } else {
            activeController = null
            updateMediaInfo()
        }
    }

    private fun setupMediaController(token: MediaSession.Token) {
        if (activeController?.sessionToken == token) {
            updateMediaInfo()
            return
        }

        activeController?.unregisterCallback(controllerCallback)
        activeController = MediaController(this, token)
        activeController?.registerCallback(controllerCallback)
        updateMediaInfo()
    }

    private fun updateMediaInfo() {
        val controller = activeController
        if (controller == null) {
            _currentMedia.value = null
            return
        }

        val metadata = controller.metadata
        val state = controller.playbackState

        var art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        if (art == null) {
            try {
                val activeNotifs = activeNotifications
                val sbn = activeNotifs?.find { it.packageName == controller.packageName }
                val iconDrawable = sbn?.notification?.getLargeIcon()?.loadDrawable(this)
                if (iconDrawable != null) {
                    art = iconDrawable.toBitmap()
                }
            } catch (e: Exception) {
            }
        }

        _currentMedia.value = MediaInfo(
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title",
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist",
            albumArt = art,
            isPlaying = state?.state == PlaybackState.STATE_PLAYING || state?.state == PlaybackState.STATE_BUFFERING,
            packageName = controller.packageName
        )
    }
}

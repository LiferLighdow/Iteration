package com.liferlighdow.iteration

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
            when (command) {
                "play_pause" -> {
                    val state = activeController?.playbackState?.state
                    if (state == PlaybackState.STATE_PLAYING) {
                        activeController?.transportControls?.pause()
                    } else {
                        activeController?.transportControls?.play()
                    }
                }
                "next" -> activeController?.transportControls?.skipToNext()
                "previous" -> activeController?.transportControls?.skipToPrevious()
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
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateCounts()
        checkMediaNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateCounts()
        if (sbn != null && sbn.packageName == activeController?.packageName) {
            val stillHasMedia = activeNotifications?.any { 
                it.packageName == sbn.packageName && it.notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION) 
            } ?: false
            
            if (!stillHasMedia) {
                activeController?.unregisterCallback(controllerCallback)
                activeController = null
                updateMediaInfo()
            }
        }
    }

    override fun onListenerConnected() {
        updateCounts()
        findActiveMediaSession()
    }

    private fun updateCounts() {
        val activeNotifications = activeNotifications ?: return
        val counts = activeNotifications
            .filter { !it.isOngoing && it.packageName != packageName }
            .groupBy { it.packageName }
            .mapValues { it.value.size }
        _notifications.value = counts
    }

    private fun checkMediaNotification(sbn: StatusBarNotification?) {
        val token = sbn?.notification?.extras?.get(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token
        if (token != null) {
            setupMediaController(token, sbn.packageName)
        } else if (sbn?.packageName == activeController?.packageName) {
            updateMediaInfo()
        }
    }

    private fun findActiveMediaSession() {
        val activeNotifications = activeNotifications ?: return
        for (sbn in activeNotifications) {
            val token = sbn.notification?.extras?.get(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token
            if (token != null) {
                setupMediaController(token, sbn.packageName)
                // Don't return yet, continue to find the one that is actually playing
                val controller = MediaController(this, token)
                if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    setupMediaController(token, sbn.packageName)
                    return
                }
            }
        }
    }

    private fun setupMediaController(token: MediaSession.Token, pkg: String) {
        // Even if same package, the token might have changed
        if (activeController?.packageName == pkg && activeController?.sessionToken == token) {
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
        
        // Try to get album art from metadata, then fallback to notification large icon
        var art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) 
               ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        if (art == null) {
            // Fallback: search notifications for this package to find a large icon
            val sbn = activeNotifications?.find { it.packageName == controller.packageName }
            val iconDrawable = sbn?.notification?.getLargeIcon()?.loadDrawable(this)
            if (iconDrawable != null) {
                try {
                    art = iconDrawable.toBitmap()
                } catch (e: Exception) {}
            }
        }

        _currentMedia.value = MediaInfo(
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title",
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist",
            albumArt = art,
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            packageName = controller.packageName
        )
    }
}

package org.antidepressants.mp5.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.antidepressants.mp5.MainActivity
import org.antidepressants.mp5.player.DemoPlayer
import org.antidepressants.mp5.player.PlaybackState

/**
 * Music playback service for Android.
 * Provides media notification with playback controls.
 */
@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {
    
    companion object {
        private const val CHANNEL_ID = "psychopath_playback_channel"
        private const val NOTIFICATION_ID = 1001
    }
    
    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Create MediaSession with the existing ExoPlayer from DemoPlayer
        // Note: This requires the player to be set up before this service starts
        android.util.Log.d("MusicPlaybackService", "Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        // Start observing player state
        scope.launch {
            DemoPlayer.controller.playerState.collectLatest { state ->
                updateNotification(state)
            }
        }
        
        return START_STICKY
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Psychopath Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun updateNotification(state: org.antidepressants.mp5.player.PlayerState) {
        val track = state.currentTrack ?: return
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val isPlaying = state.playbackState == PlaybackState.PLAYING
        
        // Play/Pause action
        val playPauseIntent = Intent(this, MusicPlaybackService::class.java).apply {
            action = if (isPlaying) "ACTION_PAUSE" else "ACTION_PLAY"
        }
        val playPausePendingIntent = PendingIntent.getService(
            this, 0, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Previous action
        val prevIntent = Intent(this, MusicPlaybackService::class.java).apply {
            action = "ACTION_PREVIOUS"
        }
        val prevPendingIntent = PendingIntent.getService(
            this, 1, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Next action
        val nextIntent = Intent(this, MusicPlaybackService::class.java).apply {
            action = "ACTION_NEXT"
        }
        val nextPendingIntent = PendingIntent.getService(
            this, 2, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep playing when app is swiped away
        val player = mediaSession?.player
        if (player?.isPlaying == true) {
            // Keep playing
        } else {
            stopSelf()
        }
    }
}

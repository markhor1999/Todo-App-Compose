package com.codingwithsalman.voicenotes.feature.capture

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground presence for an active recording: keeps the process + microphone
 * alive when the user leaves the app, and offers Stop from the notification.
 * All actual recording logic lives in [RecordingSessionManager].
 */
@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var sessionManager: RecordingSessionManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            sessionManager.stopAndSave() // manager stops this service when the session ends
            return START_NOT_STICKY
        }

        val state = sessionManager.state.value
        val notification = RecordingNotifications.build(
            context = this,
            startedAtMs = state.startedAtMs.takeIf { it > 0 } ?: System.currentTimeMillis(),
            paused = state.isPaused,
        )
        ServiceCompat.startForeground(
            this,
            RecordingNotifications.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
        )
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Task swiped away mid-recording: save what we have rather than lose the take.
        sessionManager.stopAndSave()
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        const val ACTION_STOP = "com.codingwithsalman.voicenotes.action.STOP_RECORDING"
    }
}

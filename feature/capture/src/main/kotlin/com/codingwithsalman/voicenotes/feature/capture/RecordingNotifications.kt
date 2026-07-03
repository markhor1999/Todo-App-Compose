package com.codingwithsalman.voicenotes.feature.capture

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/** One builder for the recording notification, shared by the service (startForeground)
 *  and the session manager (pause/resume updates). */
object RecordingNotifications {

    const val CHANNEL_RECORDING = "recording" // created in VoiceNotesApp
    const val NOTIFICATION_ID = 42_001

    fun build(context: Context, startedAtMs: Long, paused: Boolean): Notification {
        val stopIntent = PendingIntent.getService(
            context,
            0,
            Intent(context, RecordingService::class.java).setAction(RecordingService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.let { launch ->
                PendingIntent.getActivity(
                    context, 1, launch,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

        return NotificationCompat.Builder(context, CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.vn_ic_notif_mic)
            .setContentTitle(if (paused) "Recording paused" else "Recording voice note")
            .setContentText(if (paused) "Resume from the app" else "Tap to return · Stop to save")
            .setUsesChronometer(!paused)
            .setWhen(if (paused) System.currentTimeMillis() else startedAtMs)
            .setShowWhen(!paused)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .addAction(0, "Stop & save", stopIntent)
            .build()
    }
}

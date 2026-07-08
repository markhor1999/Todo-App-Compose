package com.codingwithsalman.voicenotes.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VoiceNotesApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** On-demand WorkManager init with Hilt-injected workers (default initializer removed in manifest). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RECORDING,
                "Recording",
                NotificationManager.IMPORTANCE_LOW, // silent, ongoing chip
            ).apply { description = "Shown while a voice note is being recorded" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TRANSCRIPTION,
                "Transcription",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Progress while notes are transcribed on this device" }
        )
    }

    companion object {
        const val CHANNEL_RECORDING = "recording"
        const val CHANNEL_TRANSCRIPTION = "transcription"
    }
}

package com.codingwithsalman.voicenotes.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.codingwithsalman.voicenotes.core.reminders.ReminderScheduler
import com.codingwithsalman.voicenotes.core.reminders.ReminderWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.codingwithsalman.voicenotes.core.designsystem.R as DsR

@HiltAndroidApp
class VoiceNotesApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** On-demand WorkManager init with Hilt-injected workers (default initializer removed in manifest). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Application.onCreate runs in EVERY process, and since 2.4.0 inference lives in its own
        // (`:asr`). Notification channels and reminder rescheduling belong to the app process
        // only — rescheduling in particular queries Room, and opening the database in a second
        // process would need multi-instance invalidation we do not want to take on. The ASR
        // process needs nothing but the model files.
        if (!isMainProcess()) return
        createNotificationChannels()
        rescheduleReminders()
    }

    private fun isMainProcess(): Boolean {
        val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()
        } else {
            val pid = android.os.Process.myPid()
            (getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager)
                ?.runningAppProcesses
                ?.firstOrNull { it.pid == pid }
                ?.processName
        }
        // A null reading should not silently disable the app's own startup work.
        return name == null || name == packageName
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
        manager.createNotificationChannel(
            NotificationChannel(
                ReminderWorker.CHANNEL_ID,
                getString(DsR.string.vn_reminder_channel_name),
                // DEFAULT, not LOW: this one is the point of the feature — a silent deadline
                // reminder is no reminder. The user opted in before any of these can fire.
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = getString(DsR.string.vn_reminder_channel_desc) }
        )
    }

    /**
     * Re-arm outstanding deadline reminders on start. WorkManager persists its own queue across
     * reboots, so this is belt-and-braces for what it can't know about: a restore onto a new device,
     * or the user moving the system clock or timezone. Unique work is REPLACEd, so it is idempotent.
     */
    private fun rescheduleReminders() {
        appScope.launch { runCatching { reminderScheduler.rescheduleAll() } }
    }

    companion object {
        const val CHANNEL_RECORDING = "recording"
        const val CHANNEL_TRANSCRIPTION = "transcription"
    }
}

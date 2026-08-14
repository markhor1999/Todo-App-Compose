package com.codingwithsalman.voicenotes.core.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.codingwithsalman.voicenotes.core.database.NotesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.codingwithsalman.voicenotes.core.designsystem.R as DsR

/**
 * Fires one action item's deadline notification. Tapping it opens the note the item came from.
 *
 * Re-reads the item before notifying, so an item completed or deleted between scheduling and firing
 * stays silent — WorkManager cannot be cancelled reliably enough to be the only guard.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: NotesRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val actionItemId = inputData.getLong(KEY_ACTION_ITEM_ID, -1L)
        val noteId = inputData.getLong(KEY_NOTE_ID, -1L)
        if (actionItemId < 0) return Result.success()

        // The item may have been ticked off or deleted since this was scheduled.
        val item = repository.actionItem(actionItemId) ?: return Result.success()
        if (item.done) return Result.success()

        val intent = applicationContext.packageManager
            ?.getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply { putExtra(EXTRA_NOTE_ID, noteId) }

        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                applicationContext,
                actionItemId.toInt(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.vn_ic_notif_reminder)
            .setContentTitle(applicationContext.getString(DsR.string.vn_reminder_notification_title))
            .setContentText(item.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .apply { pendingIntent?.let(::setContentIntent) }
            .build()

        // Best-effort: silently dropped if POST_NOTIFICATIONS was denied, same as the
        // transcription progress notification.
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_BASE + actionItemId.toInt(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_ACTION_ITEM_ID = "action_item_id"
        const val KEY_NOTE_ID = "note_id"

        /** Notification channel for due action items; created by the app on start. */
        const val CHANNEL_ID = "reminders"

        /** Intent extra carrying the note to open when a reminder is tapped. */
        const val EXTRA_NOTE_ID = "com.codingwithsalman.voicenotes.NOTE_ID"

        private const val NOTIFICATION_ID_BASE = 20_000
    }
}

package com.codingwithsalman.voicenotes.core.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.codingwithsalman.voicenotes.core.database.NotesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the one-shot notification that fires when an action item comes due.
 *
 * **Built on WorkManager, deliberately not AlarmManager.** Exact alarms would need
 * `SCHEDULE_EXACT_ALARM` (a user-granted special access on Android 12+) or `USE_EXACT_ALARM`, and
 * Play restricts the latter to apps whose *core* purpose is alarms or calendars — which Murmur is
 * not. Deadlines here are day-granularity ("Friday"), so WorkManager's inexact windows are the
 * right fit and cost no permission, no policy exposure, and no battery-optimisation prompt.
 * WorkManager also survives reboot on its own, so nothing has to be re-armed by a BOOT_COMPLETED
 * receiver.
 *
 * Work is keyed uniquely per action item, so rescheduling replaces cleanly and completing or
 * deleting an item cancels its reminder.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: NotesRepository,
) {

    private val workManager get() = WorkManager.getInstance(context)

    /**
     * Arm (or re-arm) the reminder for [actionItemId] at [dueAtMs].
     *
     * A deadline already in the past is dropped rather than fired immediately: an item extracted
     * from an old recording would otherwise notify the moment it was created, which reads as a bug.
     */
    fun schedule(actionItemId: Long, dueAtMs: Long, noteId: Long) {
        val delay = dueAtMs - System.currentTimeMillis()
        if (delay <= 0) {
            cancel(actionItemId)
            return
        }
        workManager.enqueueUniqueWork(
            workName(actionItemId),
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        ReminderWorker.KEY_ACTION_ITEM_ID to actionItemId,
                        ReminderWorker.KEY_NOTE_ID to noteId,
                    )
                )
                .build(),
        )
    }

    fun cancel(actionItemId: Long) {
        workManager.cancelUniqueWork(workName(actionItemId))
    }

    /**
     * Re-arm everything still outstanding. Cheap and idempotent (unique work is REPLACEd), so it is
     * safe to call on app start — which is what covers the cases WorkManager alone does not:
     * a restore to a new device, or the user changing the system clock or timezone.
     */
    suspend fun rescheduleAll() {
        for (item in repository.pendingReminders()) {
            val due = item.dueAtMs ?: continue
            schedule(item.id, due, item.noteId)
        }
    }

    /** Apply a due-date change end to end: persist it, then arm or cancel the notification. */
    suspend fun setDueDate(actionItemId: Long, noteId: Long, dueAtMs: Long?) {
        repository.setActionItemDue(actionItemId, dueAtMs)
        if (dueAtMs == null) cancel(actionItemId) else schedule(actionItemId, dueAtMs, noteId)
    }

    private fun workName(actionItemId: Long) = "$WORK_PREFIX$actionItemId"

    private companion object {
        const val WORK_PREFIX = "reminder-"
    }
}

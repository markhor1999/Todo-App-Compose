package com.codingwithsalman.voicenotes.core.common.util

import android.content.Context
import com.codingwithsalman.voicenotes.core.common.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Isolates an LTR fragment (times, durations) so RTL contexts can't reorder it.
 *  FSI/PDI are zero-width — invisible in LTR locales. */
private fun ltrIsolate(text: String): String = "\u2068$text\u2069"

/** 73_000 -> "1:13"; 3_723_000 -> "1:02:03". For durations and player positions. */
fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return ltrIsolate(
        if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    )
}

/** Recording timer: always mm:ss (07:03), hour-aware past 60 min. */
fun formatTimerMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/** "Today · 5:12 PM" / "Yesterday · 9:03 AM" / "Jul 3 · 5:12 PM" (adds year when it differs). */
fun formatNoteDate(context: Context, epochMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMs))
    val day = Calendar.getInstance().apply { timeInMillis = epochMs }
    val now = Calendar.getInstance().apply { timeInMillis = nowMs }

    val sameDay = day.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        day.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = day.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
        day.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)

    val isolatedTime = ltrIsolate(time)
    return when {
        sameDay -> context.getString(R.string.vn_today) + " · " + isolatedTime
        isYesterday -> context.getString(R.string.vn_yesterday) + " · " + isolatedTime
        day.get(Calendar.YEAR) == now.get(Calendar.YEAR) ->
            ltrIsolate(SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))) + " · " + isolatedTime
        else ->
            ltrIsolate(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epochMs))) + " · " + isolatedTime
    }
}

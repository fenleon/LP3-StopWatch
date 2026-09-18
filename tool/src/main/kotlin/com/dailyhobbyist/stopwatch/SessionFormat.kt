package com.dailyhobbyist.stopwatch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** e.g. "Mon, Jul 8" */
fun formatSessionDate(wallMs: Long): String {
    val fmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    return fmt.format(Date(wallMs))
}

/** e.g. "3:47 PM" */
fun formatSessionTime(wallMs: Long): String {
    val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    return fmt.format(Date(wallMs))
}

fun lapCountLabel(count: Int): String =
    if (count == 1) "1 lap" else "$count laps"

/**
 * Compact row format: leading zero units are dropped —
 * 00:01.66 → "1.66", 01:15.16 → "1:15.16", 1:02:15.16 stays as-is.
 */
internal fun compactTime(ms: Long): String {
    val clamped = if (ms < 0) 0L else ms
    val h = clamped / 3_600_000
    val m = (clamped % 3_600_000) / 60_000
    val s = (clamped % 60_000) / 1_000
    val c = (clamped % 1_000) / 10
    return when {
        h > 0 -> "%d:%02d:%02d.%02d".format(h, m, s, c)
        m > 0 -> "%d:%02d.%02d".format(m, s, c)
        else -> "%d.%02d".format(s, c)
    }
}

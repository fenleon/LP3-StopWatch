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

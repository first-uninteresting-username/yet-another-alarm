package io.github.first_uninteresting_username.yetanotheralarm

import android.content.Context
import android.text.format.DateFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale

/** Formats a wall-clock time the way the user set the system clock (12 or 24 hour). */
fun formatTime(context: Context, hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}

fun formatTime(context: Context, epochMillis: Long): String =
    DateFormat.getTimeFormat(context).format(epochMillis)

/** The seven days of the week, starting on the locale's first day. */
fun weekDays(): List<DayOfWeek> {
    val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    return (0L until 7L).map { first.plus(it) }
}

fun formatDays(days: Set<Int>): String = when {
    days.isEmpty() -> "Once"
    days.size == 7 -> "Every day"
    else -> weekDays()
        .filter { it.value in days }
        .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

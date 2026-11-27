package io.github.first_uninteresting_username.yetanotheralarm

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class Alarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    /** ISO day-of-week numbers (1 = Monday). Empty means the alarm rings once. */
    val days: Set<Int> = emptySet(),
    val soundUri: String,
    val soundName: String,
    val snoozeMinutes: Int = 10,
    val enabled: Boolean = true,
    /** Epoch millis of a pending snooze, or null. */
    val snoozedUntil: Long? = null,
) {
    /** Epoch millis of the next time this alarm should ring, or null if it never will. */
    fun nextTrigger(now: ZonedDateTime): Long? {
        val nowMillis = now.toInstant().toEpochMilli()
        val snooze = snoozedUntil?.takeIf { it > nowMillis }
        val regular = if (enabled) nextOccurrence(now) else null
        return listOfNotNull(snooze, regular).minOrNull()
    }

    private fun nextOccurrence(now: ZonedDateTime): Long {
        var t = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!t.isAfter(now)) t = t.plusDays(1)
        if (days.isNotEmpty()) {
            while (t.dayOfWeek.value !in days) t = t.plusDays(1)
        }
        return t.toInstant().toEpochMilli()
    }
}

package io.github.first_uninteresting_username.yetanotheralarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import java.time.ZonedDateTime

object AlarmScheduler {
    /** Brings AlarmManager in line with the store. Safe to call any time. */
    fun sync(context: Context) {
        val manager = context.getSystemService<AlarmManager>() ?: return
        if (!manager.canScheduleExactAlarms()) return
        val now = ZonedDateTime.now()
        for (alarm in AlarmStore.get(context).alarms.value) {
            val trigger = alarm.nextTrigger(now)
            val operation = triggerIntent(context, alarm.id)
            if (trigger == null) {
                manager.cancel(operation)
            } else {
                manager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, showIntent(context)), operation)
            }
        }
    }

    fun cancel(context: Context, id: Long) {
        context.getSystemService<AlarmManager>()?.cancel(triggerIntent(context, id))
    }

    private fun triggerIntent(context: Context, id: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AlarmReceiver::class.java)
                .setData("alarm:$id".toUri())
                .putExtra(AlarmService.EXTRA_ALARM_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun showIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
}

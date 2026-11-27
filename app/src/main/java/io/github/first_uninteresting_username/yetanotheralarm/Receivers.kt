package io.github.first_uninteresting_username.yetanotheralarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmService.EXTRA_ALARM_ID, -1)
        if (id < 0) return
        context.startForegroundService(
            Intent(context, AlarmService::class.java).putExtra(AlarmService.EXTRA_ALARM_ID, id),
        )
    }
}

/** Re-registers alarms after reboot, app update, clock or time zone changes, and permission grants. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in ACTIONS) AlarmScheduler.sync(context)
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}

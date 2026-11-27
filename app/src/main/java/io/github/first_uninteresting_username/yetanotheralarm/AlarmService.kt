package io.github.first_uninteresting_username.yetanotheralarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Plays the ringing alarm in the foreground until it is snoozed or stopped. */
class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getLongExtra(EXTRA_ALARM_ID, -1) ?: -1
        when (intent?.action) {
            ACTION_SNOOZE -> {
                snooze(id)
                stopRinging()
            }
            ACTION_STOP -> stopRinging()
            else -> ring(id)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releasePlayback()
        ringingState.value = null
        super.onDestroy()
    }

    private fun ring(id: Long) {
        val store = AlarmStore.get(this)
        val alarm = store.get(id)
        // Set before the notification goes up: its full-screen intent opens
        // RingActivity, which closes itself when nothing is ringing.
        ringingState.value = alarm?.id
        startForeground(
            NOTIFICATION_ID,
            notification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        if (alarm == null) {
            stopRinging()
            return
        }
        store.edit(id) { it.copy(snoozedUntil = null, enabled = it.enabled && it.days.isNotEmpty()) }
        AlarmScheduler.sync(this)

        releasePlayback()
        wakeLock = getSystemService<PowerManager>()
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "yetanotheralarm:ringing")
            ?.apply { acquire(WAKE_LOCK_TIMEOUT_MS) }
        player = play(alarm)
        if (player == null) vibrate()
    }

    private fun play(alarm: Alarm): MediaPlayer? {
        val mediaPlayer = MediaPlayer()
        return try {
            mediaPlayer.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(this@AlarmService, alarm.soundUri.toUri())
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            mediaPlayer.release()
            null
        }
    }

    private fun vibrate() {
        val v = getSystemService<VibratorManager>()?.defaultVibrator ?: return
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
        vibrator = v
    }

    private fun snooze(id: Long) {
        AlarmStore.get(this).edit(id) {
            it.copy(snoozedUntil = System.currentTimeMillis() + it.snoozeMinutes * 60_000L)
        }
        AlarmScheduler.sync(this)
    }

    private fun stopRinging() {
        releasePlayback()
        ringingState.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayback() {
        player?.release()
        player = null
        vibrator?.cancel()
        vibrator = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun notification(): Notification {
        val manager = getSystemService<NotificationManager>()!!
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Ringing alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
            },
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, RingActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Alarm")
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        private const val ACTION_SNOOZE = "snooze"
        private const val ACTION_STOP = "stop"
        private const val CHANNEL_ID = "ringing"
        private const val NOTIFICATION_ID = 1
        private const val WAKE_LOCK_TIMEOUT_MS = 60 * 60 * 1000L

        private val ringingState = MutableStateFlow<Long?>(null)

        /** Id of the alarm that is ringing right now, or null. */
        val ringing: StateFlow<Long?> = ringingState

        fun snooze(context: Context, id: Long) = context.startService(
            Intent(context, AlarmService::class.java).setAction(ACTION_SNOOZE).putExtra(EXTRA_ALARM_ID, id),
        )

        fun stop(context: Context) = context.startService(
            Intent(context, AlarmService::class.java).setAction(ACTION_STOP),
        )
    }
}

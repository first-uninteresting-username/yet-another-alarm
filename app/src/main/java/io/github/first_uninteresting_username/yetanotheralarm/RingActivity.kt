package io.github.first_uninteresting_username.yetanotheralarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.first_uninteresting_username.yetanotheralarm.ui.AppTheme
import io.github.first_uninteresting_username.yetanotheralarm.ui.RingScreen

/** Shown over the lock screen while an alarm rings. */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Back must not hide a ringing alarm.
        onBackPressedDispatcher.addCallback(this) {}

        val store = AlarmStore.get(this)
        setContent {
            AppTheme {
                val ringingId by AlarmService.ringing.collectAsStateWithLifecycle()
                val alarms by store.alarms.collectAsStateWithLifecycle()
                val alarm = alarms.find { it.id == ringingId }
                LaunchedEffect(ringingId) {
                    if (ringingId == null) finish()
                }
                if (alarm != null) {
                    RingScreen(
                        alarm = alarm,
                        onSnooze = { AlarmService.snooze(this, alarm.id) },
                        onStop = { AlarmService.stop(this) },
                    )
                }
            }
        }
    }
}

package io.github.first_uninteresting_username.yetanotheralarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.first_uninteresting_username.yetanotheralarm.ui.AlarmsScreen
import io.github.first_uninteresting_username.yetanotheralarm.ui.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AlarmScheduler.sync(this)
        setContent {
            AppTheme {
                AlarmsScreen(AlarmStore.get(this))
            }
        }
    }
}

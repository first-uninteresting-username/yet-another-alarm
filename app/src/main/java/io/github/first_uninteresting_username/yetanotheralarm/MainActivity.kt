package io.github.first_uninteresting_username.yetanotheralarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.first_uninteresting_username.yetanotheralarm.ui.AboutScreen
import io.github.first_uninteresting_username.yetanotheralarm.ui.AlarmsScreen
import io.github.first_uninteresting_username.yetanotheralarm.ui.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AlarmScheduler.sync(this)
        setContent {
            AppTheme {
                var showAbout by rememberSaveable { mutableStateOf(false) }
                if (showAbout) {
                    BackHandler { showAbout = false }
                    AboutScreen(onBack = { showAbout = false })
                } else {
                    AlarmsScreen(AlarmStore.get(this), onShowAbout = { showAbout = true })
                }
            }
        }
    }
}

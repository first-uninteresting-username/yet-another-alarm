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
import io.github.first_uninteresting_username.yetanotheralarm.ui.SettingsScreen

private enum class Screen { Alarms, Settings, About }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AlarmScheduler.sync(this)
        setContent {
            AppTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.Alarms) }
                when (screen) {
                    Screen.Alarms -> AlarmsScreen(
                        AlarmStore.get(this),
                        Settings.get(this),
                        onShowSettings = { screen = Screen.Settings },
                    )
                    Screen.Settings -> {
                        BackHandler { screen = Screen.Alarms }
                        SettingsScreen(
                            Settings.get(this),
                            onBack = { screen = Screen.Alarms },
                            onShowAbout = { screen = Screen.About },
                        )
                    }
                    Screen.About -> {
                        BackHandler { screen = Screen.Settings }
                        AboutScreen(onBack = { screen = Screen.Settings })
                    }
                }
            }
        }
    }
}

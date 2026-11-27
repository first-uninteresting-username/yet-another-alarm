package io.github.first_uninteresting_username.yetanotheralarm.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.first_uninteresting_username.yetanotheralarm.R
import io.github.first_uninteresting_username.yetanotheralarm.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: Settings, onBack: () -> Unit, onShowAbout: () -> Unit) {
    val sound by settings.defaultSound.collectAsStateWithLifecycle()
    val pickSound = rememberSoundPicker { settings.setDefaultSound(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            ListItem(
                onClick = pickSound,
                supportingContent = {
                    Text(
                        sound?.name ?: "None. New alarms start without a sound.",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingContent = { Icon(painterResource(R.drawable.ic_music_note), contentDescription = null) },
                trailingContent = sound?.let {
                    {
                        IconButton(onClick = { settings.setDefaultSound(null) }) {
                            Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear default sound")
                        }
                    }
                },
            ) { Text("Default sound") }
            ListItem(
                onClick = onShowAbout,
                leadingContent = { Icon(painterResource(R.drawable.ic_info), contentDescription = null) },
            ) { Text("About and licenses") }
        }
    }
}

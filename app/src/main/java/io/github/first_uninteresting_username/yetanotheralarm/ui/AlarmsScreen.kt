package io.github.first_uninteresting_username.yetanotheralarm.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.first_uninteresting_username.yetanotheralarm.Alarm
import io.github.first_uninteresting_username.yetanotheralarm.AlarmScheduler
import io.github.first_uninteresting_username.yetanotheralarm.AlarmStore
import io.github.first_uninteresting_username.yetanotheralarm.R
import io.github.first_uninteresting_username.yetanotheralarm.formatDays
import io.github.first_uninteresting_username.yetanotheralarm.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface Editing {
    data object New : Editing
    data class Existing(val alarm: Alarm) : Editing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(store: AlarmStore, onShowAbout: () -> Unit) {
    val context = LocalContext.current
    val alarms by store.alarms.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Editing?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Alarms") },
                actions = {
                    IconButton(onClick = onShowAbout) {
                        Icon(painterResource(R.drawable.ic_info), contentDescription = "About and licenses")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            MediumExtendedFloatingActionButton(onClick = { editing = Editing.New }) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                Text("Add alarm")
            }
        },
    ) { padding ->
        val direction = LocalLayoutDirection.current
        LazyColumn(
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(direction) + 16.dp,
                end = padding.calculateEndPadding(direction) + 16.dp,
                top = padding.calculateTopPadding(),
                // Leave room so the FAB never covers the last alarm's switch.
                bottom = padding.calculateBottomPadding() + 112.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PermissionsCard() }
            if (alarms.isEmpty()) {
                item {
                    Text(
                        "No alarms yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(alarms.sortedBy { it.hour * 60 + it.minute }, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onClick = { editing = Editing.Existing(alarm) },
                    onToggle = { on ->
                        store.edit(alarm.id) { it.copy(enabled = on, snoozedUntil = null) }
                        AlarmScheduler.sync(context)
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    editing?.let { current ->
        AlarmEditor(
            initial = (current as? Editing.Existing)?.alarm,
            onDismiss = { editing = null },
            onSave = { alarm ->
                store.upsert(alarm.copy(enabled = true, snoozedUntil = null))
                AlarmScheduler.sync(context)
                editing = null
            },
            onDelete = { id ->
                store.delete(id)
                AlarmScheduler.cancel(context, id)
                editing = null
            },
        )
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, onClick: () -> Unit, onToggle: (Boolean) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val soundReadable by produceState(true, alarm.soundUri) {
        value = withContext(Dispatchers.IO) { isReadable(context, alarm.soundUri) }
    }
    val active = alarm.enabled || alarm.snoozedUntil != null

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = if (active) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        },
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatTime(context, alarm.hour, alarm.minute), style = MaterialTheme.typography.displayMedium)
                Text(formatDays(alarm.days), style = MaterialTheme.typography.titleSmall)
                alarm.snoozedUntil?.takeIf { it > System.currentTimeMillis() }?.let {
                    Text("Snoozed until ${formatTime(context, it)}", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Inherit the card's content color so the text contrasts with either card color.
                    val color = if (soundReadable) LocalContentColor.current else MaterialTheme.colorScheme.error
                    Icon(
                        painterResource(if (soundReadable) R.drawable.ic_music_note else R.drawable.ic_warning),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = color,
                    )
                    Text(
                        if (soundReadable) alarm.soundName else "Sound file missing, will only vibrate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Switch(checked = active, onCheckedChange = onToggle)
        }
    }
}

private fun isReadable(context: Context, uri: String): Boolean =
    runCatching { context.contentResolver.openFileDescriptor(uri.toUri(), "r")?.use { true } }
        .getOrNull() ?: false

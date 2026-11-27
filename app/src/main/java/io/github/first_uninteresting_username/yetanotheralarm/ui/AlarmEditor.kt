package io.github.first_uninteresting_username.yetanotheralarm.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.first_uninteresting_username.yetanotheralarm.Alarm
import io.github.first_uninteresting_username.yetanotheralarm.R
import io.github.first_uninteresting_username.yetanotheralarm.weekDays
import java.time.format.TextStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditor(
    initial: Alarm?,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val context = LocalContext.current
    val time = rememberTimePickerState(initialHour = initial?.hour ?: 7, initialMinute = initial?.minute ?: 0)
    var days by rememberSaveable { mutableStateOf(initial?.days ?: emptySet()) }
    var soundUri by rememberSaveable { mutableStateOf(initial?.soundUri) }
    var soundName by rememberSaveable { mutableStateOf(initial?.soundName) }
    val snooze = rememberSliderState(
        value = (initial?.snoozeMinutes ?: 10).toFloat(),
        steps = 28,
        trackRange = 1f..30f,
    )

    val pickSound = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            soundUri = uri.toString()
            soundName = displayName(context, uri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        // The clock dial is dragged too; sheet drags would fight it.
        sheetGesturesEnabled = false,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TimePicker(time, Modifier.align(Alignment.CenterHorizontally))

            DayPicker(days, onChange = { days = it })

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sound", style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(
                    onClick = { pickSound.launch(arrayOf("audio/*")) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(painterResource(R.drawable.ic_music_note), contentDescription = null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(soundName ?: "Choose a sound file", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Column {
                Text("Snooze: ${snooze.value.roundToInt()} min", style = MaterialTheme.typography.titleMedium)
                Slider(state = snooze, onValueChange = { snooze.value = it })
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (initial != null) {
                    TextButton(onClick = { onDelete(initial.id) }, shapes = ButtonDefaults.shapes()) {
                        Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Delete")
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(
                            Alarm(
                                id = initial?.id ?: System.currentTimeMillis(),
                                hour = time.hour,
                                minute = time.minute,
                                days = days,
                                soundUri = soundUri ?: return@Button,
                                soundName = soundName ?: "",
                                snoozeMinutes = snooze.value.roundToInt(),
                            ),
                        )
                    },
                    enabled = soundUri != null,
                    shapes = ButtonDefaults.shapes(),
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun DayPicker(days: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val week = weekDays()
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        week.forEachIndexed { index, day ->
            val checked = day.value in days
            ToggleButton(
                checked = checked,
                onCheckedChange = { onChange(if (it) days + day.value else days - day.value) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    week.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = day.getDisplayName(TextStyle.FULL, locale) },
            ) {
                Text(day.getDisplayName(TextStyle.NARROW, locale))
            }
        }
    }
}

private fun displayName(context: Context, uri: Uri): String =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null }
        ?: uri.lastPathSegment
        ?: "Sound file"

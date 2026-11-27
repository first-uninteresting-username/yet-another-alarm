package io.github.first_uninteresting_username.yetanotheralarm.ui

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.first_uninteresting_username.yetanotheralarm.AlarmScheduler
import io.github.first_uninteresting_username.yetanotheralarm.R

private class Missing(val reason: String, val settings: Intent)

/** Lists the system settings that would stop an alarm from ringing, each with a button to fix it. */
@Composable
fun PermissionsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        AlarmScheduler.sync(context)
        onPauseOrDispose {}
    }
    val missing = remember(resumes) { missingPermissions(context) }
    if (missing.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(R.drawable.ic_warning), contentDescription = null)
                Text("Alarms may not ring", style = MaterialTheme.typography.titleMedium)
            }
            missing.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(item.reason, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    FilledTonalButton(
                        onClick = { context.startActivity(item.settings) },
                        shapes = ButtonDefaults.shapes(),
                    ) { Text("Allow") }
                }
            }
        }
    }
}

@SuppressLint("BatteryLife")
private fun missingPermissions(context: Context): List<Missing> = buildList {
    val packageUri = "package:${context.packageName}".toUri()
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        add(
            Missing(
                "Notifications are needed to show a ringing alarm.",
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            ),
        )
    }
    if (context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() == false) {
        add(
            Missing(
                "Exact alarms are needed to ring on time.",
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri),
            ),
        )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        context.getSystemService<NotificationManager>()?.canUseFullScreenIntent() == false
    ) {
        add(
            Missing(
                "Full-screen alerts are needed to show the alarm over the lock screen.",
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri),
            ),
        )
    }
    if (context.getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(context.packageName) == false) {
        add(
            Missing(
                "Battery optimization can delay or kill alarms.",
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri),
            ),
        )
    }
}

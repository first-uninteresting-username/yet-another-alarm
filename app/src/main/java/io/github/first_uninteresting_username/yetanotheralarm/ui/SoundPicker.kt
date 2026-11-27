package io.github.first_uninteresting_username.yetanotheralarm.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.first_uninteresting_username.yetanotheralarm.Sound

/** Returns a function that opens the system file picker for an audio file and keeps read access to it. */
@Composable
fun rememberSoundPicker(onPicked: (Sound) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            onPicked(Sound(uri.toString(), displayName(context, uri)))
        }
    }
    return { launcher.launch(arrayOf("audio/*")) }
}

private fun displayName(context: Context, uri: Uri): String =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null }
        ?: uri.lastPathSegment
        ?: "Sound file"

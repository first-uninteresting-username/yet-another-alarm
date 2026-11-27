package io.github.first_uninteresting_username.yetanotheralarm

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Sound(val uri: String, val name: String)

class Settings private constructor(context: Context) {
    private val prefs = context.createDeviceProtectedStorageContext()
        .getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val sound = MutableStateFlow(readDefaultSound())

    /** Sound new alarms start with. Changing it leaves existing alarms alone. */
    val defaultSound: StateFlow<Sound?> = sound

    fun setDefaultSound(value: Sound?) {
        prefs.edit {
            if (value == null) {
                remove(KEY_SOUND_URI)
                remove(KEY_SOUND_NAME)
            } else {
                putString(KEY_SOUND_URI, value.uri)
                putString(KEY_SOUND_NAME, value.name)
            }
        }
        sound.value = value
    }

    private fun readDefaultSound(): Sound? {
        val uri = prefs.getString(KEY_SOUND_URI, null) ?: return null
        return Sound(uri, prefs.getString(KEY_SOUND_NAME, null).orEmpty())
    }

    companion object {
        private const val KEY_SOUND_URI = "default_sound_uri"
        private const val KEY_SOUND_NAME = "default_sound_name"

        @Volatile private var instance: Settings? = null

        fun get(context: Context): Settings = instance ?: synchronized(this) {
            instance ?: Settings(context.applicationContext).also { instance = it }
        }
    }
}

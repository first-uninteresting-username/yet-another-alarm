package io.github.first_uninteresting_username.yetanotheralarm

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException

/**
 * Alarms persisted as JSON in device-protected storage, so they can be
 * rescheduled and rung after a reboot before the user unlocks the phone.
 */
class AlarmStore private constructor(context: Context) {
    private val file = AtomicFile(
        File(context.createDeviceProtectedStorageContext().filesDir, "alarms.json"),
    )
    private val json = Json { ignoreUnknownKeys = true }
    private val state = MutableStateFlow(load())

    val alarms: StateFlow<List<Alarm>> = state

    fun get(id: Long): Alarm? = state.value.find { it.id == id }

    @Synchronized
    fun update(transform: (List<Alarm>) -> List<Alarm>) {
        val next = transform(state.value)
        val out = file.startWrite()
        try {
            out.write(json.encodeToString(next).toByteArray())
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
        state.value = next
    }

    fun upsert(alarm: Alarm) = update { list ->
        if (list.any { it.id == alarm.id }) list.map { if (it.id == alarm.id) alarm else it }
        else list + alarm
    }

    fun edit(id: Long, change: (Alarm) -> Alarm) = update { list ->
        list.map { if (it.id == id) change(it) else it }
    }

    fun delete(id: Long) = update { list -> list.filterNot { it.id == id } }

    private fun load(): List<Alarm> = try {
        json.decodeFromString(file.readFully().decodeToString())
    } catch (_: FileNotFoundException) {
        emptyList()
    }

    companion object {
        @Volatile private var instance: AlarmStore? = null

        fun get(context: Context): AlarmStore = instance ?: synchronized(this) {
            instance ?: AlarmStore(context.applicationContext).also { instance = it }
        }
    }
}

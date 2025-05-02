package com.molten.simplesoundboard.presentation

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import kotlinx.serialization.json.Json
import java.io.File
import androidx.core.net.toUri
import kotlinx.serialization.encodeToString

class SoundboardViewModel : ViewModel() {
    private val _soundItems = mutableStateListOf<SoundButtonData>()
    val soundItems: SnapshotStateList<SoundButtonData> get() = _soundItems

    init {
        _soundItems.add(SoundButtonData(id = Int.MAX_VALUE, label = "ADD_BUTTON"))
    }

    fun loadFromFile(context: Context) {
        val file = File(context.filesDir, "soundboard_data.json")
        if (!file.exists()) return

        val jsonString = file.readText()

        val loadedSounds = Json.decodeFromString<List<SoundButtonSerializable>>(jsonString)

        for (item in loadedSounds) {
            _soundItems.add(
                SoundButtonData(
                    id = item.id,
                    label = item.label,
                    imageUri = item.imageUri?.toUri(),
                    audioUri = item.audioUri?.toUri()
                )
            )
        }
    }

    fun addSound(label: String, imageUri: Uri?, audioUri: Uri?): SoundButtonData {
        val data = SoundButtonData(
            id = getNextId(),
            label = label,
            imageUri = imageUri,
            audioUri = audioUri
        )
        _soundItems.add(data)
        return data
    }

    private fun getNextId(): Int {
        var toRet = 0
        for (soundButtonData in _soundItems) {
            if (soundButtonData.id > toRet) toRet = soundButtonData.id
        }
        return ++toRet
    }

    fun saveSoundToFile(context: Context, sound: SoundButtonData) {
        if (sound.id == Int.MAX_VALUE) return // Never save the ADD_BUTTON

        val file = File(context.filesDir, "soundboard_data.json")

        val existingSounds = if (file.exists()) {
            val jsonString = file.readText()
            Json.decodeFromString<List<SoundButtonSerializable>>(jsonString).toMutableList()
        } else {
            mutableListOf()
        }

        existingSounds.add(
            SoundButtonSerializable(
                id = sound.id,
                label = sound.label,
                imageUri = sound.imageUri?.toString(),
                audioUri = sound.audioUri?.toString()
            )
        )

        val newJsonString = Json.encodeToString(existingSounds)
        file.writeText(newJsonString)
    }
}

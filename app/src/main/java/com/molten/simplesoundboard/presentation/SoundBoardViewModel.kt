package com.molten.simplesoundboard.presentation

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import kotlinx.serialization.json.Json
import java.io.File

class SoundboardViewModel : ViewModel() {
    private val _soundItems = mutableStateListOf<SoundButtonData>()
    val soundItems: SnapshotStateList<SoundButtonData> get() = _soundItems

    init {
        val context = LocalContext.current

        val file = File(context.filesDir, "soundboard_data.json")
        if (file.exists()) {
            val jsonString = file.readText()

            val loadedSounds = Json.decodeFromString<List<SoundButtonSerializable>>(jsonString)

            _soundItems.clear()

            for (item in loadedSounds) {
                _soundItems.add(
                    SoundButtonData(
                        id = item.id,
                        label = item.label,
                        imageUri = item.imageUri?.let { Uri.parse(it) },
                        soundUri = item.soundUri?.let { Uri.parse(it) }
                    )
                )
            }
        }



        // Always add the ADD_BUTTON at the end
        _soundItems.add(SoundButtonData(id = Int.MAX_VALUE, label = "ADD_BUTTON"))
    }

    fun addSound(label: String, imageUri: Uri?, soundUri: Uri?) : SoundButtonData {
        val data = SoundButtonData(
            id = getNextId(),
            label = label,
            imageUri = imageUri,
            soundUri = soundUri
        )

        _soundItems.add(data)
        return data
    }

    private fun getNextId() : Int {
        var toRet = 0

        for (soundButtonData in _soundItems) {
            if (soundButtonData.id > toRet) toRet = soundButtonData.id
        }

        return ++toRet;
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
                soundUri = sound.soundUri?.toString()
            )
        )

        val newJsonString = Json.encodeToString(existingSounds)
        file.writeText(newJsonString)
    }


}

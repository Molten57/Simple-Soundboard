package com.molten.simplesoundboard.presentation

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
data class SoundButtonData(
    val id: Int,
    val label: String,
    val imageUri: Uri? = null,
    val soundUri: Uri? = null
)

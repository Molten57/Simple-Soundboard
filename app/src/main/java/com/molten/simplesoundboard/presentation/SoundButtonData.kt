package com.molten.simplesoundboard.presentation

import android.net.Uri

data class SoundButtonData(
    val id: Int,
    val label: String,
    val imageUri: Uri? = null,
    val audioUri: Uri? = null
)

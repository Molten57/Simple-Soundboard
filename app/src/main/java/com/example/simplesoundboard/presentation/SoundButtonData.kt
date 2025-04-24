package com.example.simplesoundboard.presentation

import android.net.Uri

data class SoundButtonData(
    val id: Int, // unique ID for the button
    val label: String, // the text shown below the image
    val imageResId: Int, // drawable resource ID (e.g. R.drawable.my_image)
    val soundUri: Uri // the URI to the sound file
)

package com.molten.simplesoundboard.presentation

import kotlinx.serialization.Serializable

@Serializable
data class SoundButtonSerializable(
    val id: Int,
    val label: String,
    val imageUri: String?,
    val audioUri: String?
)

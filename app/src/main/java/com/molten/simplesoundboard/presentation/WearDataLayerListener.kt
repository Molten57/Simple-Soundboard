package com.molten.simplesoundboard.presentation

import android.net.Uri

object WearDataLayerListener {
    var onImageReceived: ((Uri) -> Unit)? = null
    var onAudioReceived: ((Uri) -> Unit)? = null
}
package com.molten.simplesoundboardcompanion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toFile
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.molten.simplesoundboardcompanion.ui.theme.SimpleSoundboardTheme

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickAudioLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register Wearable message listener
        Wearable.getMessageClient(this).addListener(this)

        // Set up file pickers
        setupFilePickers()

        enableEdgeToEdge()
        setContent {
            SimpleSoundboardTheme {

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
    }

    // Listen for message from the watch
    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            "/request_image_upload" -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                }
                pickImageLauncher.launch(intent)
            }

            "/request_audio_upload" -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "audio/*"
                }
                pickAudioLauncher.launch(intent)
            }
        }
    }

    private fun setupFilePickers() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                sendAssetToWatch("image", uri)
            }
        }

        pickAudioLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                sendAssetToWatch("audio", uri)
            }
        }
    }

    private fun sendAssetToWatch(assetKey: String, uri: Uri) {
        val bytes = contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: run {
            Log.e("aa", "Failed to open Uri")
            return
        }
        Log.d("aa", "Bytes read: ${bytes.size}")

        if (bytes.isEmpty()) {
            Log.e("aa", "Bytes are empty! Asset not sent.")
            return
        }

        val asset = Asset.createFromBytes(bytes)

        val request = PutDataMapRequest.create("/upload_result").apply {
            dataMap.putAsset(assetKey, asset)
            dataMap.putString("type", assetKey)
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(this).putDataItem(request)
    }

}
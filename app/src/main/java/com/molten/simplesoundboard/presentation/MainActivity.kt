package com.molten.simplesoundboard.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.foundation.lazy.items
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.molten.simplesoundboard.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Log.d("aa", "startup")
            SoundboardApp()
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    @SuppressLint("VisibleForTests")
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            Log.d("aa", event.dataItem.uri.path.toString());
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/upload_result") {
                Log.d("aa", "mega 2")
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val type = dataMap.getString("type")
                val asset = dataMap.getAsset(type.toString())

                asset?.let {
                    val inputStreamTask = Wearable.getDataClient(this).getFdForAsset(it)

                    inputStreamTask.addOnSuccessListener { result ->
                        val inputStream = result.inputStream

                        Log.d("aa", "InputStream available: ${inputStream.available()} bytes")

                        val context = this // Activity is a Context
                        val uuid = UUID.randomUUID()
                        val fileName = if (type == "image") "${uuid}_uploaded_image" else "${uuid}_uploaded_audio"

                        val subDir = File(context.filesDir, type + "s")
                        if (!subDir.exists()) subDir.mkdirs()

                        val file = File(subDir, fileName)

                        val output = FileOutputStream(file)
                        inputStream.copyTo(output)

                        val fileUri = file.toUri()

                        if (type == "image") {
                            WearDataLayerListener.onImageReceived?.invoke(fileUri)
                        } else if (type == "audio") {
                            WearDataLayerListener.onAudioReceived?.invoke(fileUri)
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun SoundboardApp() {
    val navController = rememberNavController()
    val viewModel: SoundboardViewModel = viewModel()
    val context = LocalContext.current

    viewModel.loadFromFile(context)

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            SoundBoardScreen(
                viewModel = viewModel,
                onAddButtonClicked = {
                    navController.navigate("add")
                }
            )
        }
        composable("add") {
            AddSoundButtonScreen(
                navController,

                onSelectImage = {
                    navController.currentBackStackEntry?.savedStateHandle?.remove<Uri>("selectedImage")
                    navController.navigate("image_selection")
                },

                onSelectAudio = {
//                    navController.currentBackStackEntry?.savedStateHandle?.remove<Uri>("selectedAudio")
//                    navController.navigate("audio_selection")
                },

                onConfirm = { label, imageUri, audioUri ->
                    val addedSound = viewModel.addSound(label, imageUri, audioUri)
                    viewModel.saveSoundToFile(context, addedSound)
                    navController.popBackStack()
                }
            )
        }
        composable("image_selection") {
            AssetSelection("image", navController)
        }
//        composable("audio_selection") {
//            AssetSelection("audio", navController)
//        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SoundBoardScreen(
    viewModel: SoundboardViewModel,
    itemsPerRow: Int = 3,
    onAddButtonClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester: FocusRequester = remember { FocusRequester() }

    val soundItems = viewModel.soundItems.sortedBy { it.id }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ScalingLazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(25.dp),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    end = 10.dp
                ),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .onRotaryScrollEvent {
                        val scrollDelta = it.verticalScrollPixels * 0.5f
                        coroutineScope.launch {
                            listState.animateScrollBy(scrollDelta)
                        }
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable()
            ) {
                item {
                    VolumeButton(LocalContext.current)
                }

                val rows = soundItems.chunked(itemsPerRow)

                items(rows.size) { rowIndex ->
                    val rowItems = rows[rowIndex]

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val buttonSpacing = 8.dp
                        val totalSpacing = buttonSpacing * (itemsPerRow - 1)
                        val buttonSize = (maxWidth - totalSpacing) / itemsPerRow

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowItems.forEach { sound ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(buttonSize)
                                ) {
                                    if (sound.id == Int.MAX_VALUE) {
                                        AddButton(buttonSize) { onAddButtonClicked() }
                                    } else {
                                        Button(
                                            onClick = {
                                                playSound(context, sound.audioUri)
                                            },
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .size(buttonSize)
                                                .padding(2.dp),
                                            contentPadding = PaddingValues(0.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)  // Ensures the image fits inside the button's circular shape
                                            ) {
                                                AsyncImage(
                                                    model = sound.imageUri,  // The URI of the image to load
                                                    contentDescription = "Sound icon",  // Description for accessibility
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop  // Ensures the image is cropped to fit inside the circle
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = sound.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            fontSize = 10.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Fill remaining space with empty Spacers
                            repeat(itemsPerRow - rowItems.size) {
                                Spacer(modifier = Modifier.width(buttonSize))
                            }
                        }
                    }

                }
            }

            Scaffold {
                PositionIndicator(
                    scalingLazyListState = listState
                )
            }
        }
    }
}

fun playSound(context: Context, audioUri: Uri?) {
    val mediaPlayer = MediaPlayer()

    val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    mediaPlayer.reset()
    mediaPlayer.setAudioAttributes(audioAttributes)
    audioUri?.let {
        mediaPlayer.setDataSource(
            context,
            it
        )
    }
    mediaPlayer.prepare()
    mediaPlayer.start()
}

@Composable
fun AddSoundButtonScreen(
    navController: NavController,
    onSelectImage: () -> Unit,
    onSelectAudio: () -> Unit,
    onConfirm: (label: String, imageUri: Uri?, audioUri: Uri?) -> Unit
) {
    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester: FocusRequester = remember { FocusRequester() }


    var label by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    val isFormValid = label.isNotBlank() && imageUri != null && audioUri != null
    var showValidation by remember { mutableStateOf(false) }


    val currentBackStackEntry = remember { navController.currentBackStackEntry }

    LaunchedEffect(currentBackStackEntry) {
        currentBackStackEntry?.savedStateHandle?.getLiveData<Uri>("selectedImage")
            ?.observeForever { uri ->
                imageUri = uri
                currentBackStackEntry.savedStateHandle.remove<Uri>("selectedImage") // Prevent re-trigger
            }
    }
//
//    LaunchedEffect(currentBackStackEntry) {
//        currentBackStackEntry?.savedStateHandle?.getLiveData<Uri>("selectedAudio")
//            ?.observeForever { uri ->
//                audioUri = uri
//                currentBackStackEntry.savedStateHandle.remove<Uri>("selectedAudio") // Prevent re-trigger
//            }
//    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ScalingLazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(25.dp),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    end = 10.dp
                ),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .onRotaryScrollEvent {
                        val scrollDelta = it.verticalScrollPixels * 0.5f
                        coroutineScope.launch {
                            listState.animateScrollBy(scrollDelta)
                        }
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable()
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Sound Name",
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = label,
                            onValueChange = {
                                label = it.replace("\n", "").take(25)
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            colors = TextFieldDefaults.colors(
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White,
                                unfocusedContainerColor = Color.Black,
                                focusedContainerColor = Color.Black,
                                focusedIndicatorColor = Color(0xFF4285F4),
                                unfocusedIndicatorColor = Color(0xFF4285F4),
                                cursorColor = Color.White
                            ),
                            singleLine = true
                        )
                    }
                }

                if (showValidation && label.isBlank()) {
                    item {
                        Text(
                            text = "Name is required",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                item {
                    Button(
                        onClick = {
                            onSelectImage()
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.add_photo),
                                contentDescription = "Pick an image",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Pick Image",
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                        }
                    }
                }

                if (showValidation && imageUri == null) {
                    item {
                        Text(
                            text = "Image is required",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }



                if (imageUri != null) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(imageUri),
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                            IconButton(onClick = { imageUri = null }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove image",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }

                // Audio field, item 5
                item {
                    Button(
                        onClick = {
//                            onSelectAudio()
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.volume_up),
                                contentDescription = "Pick an audio",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Pick Audio",
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                        }
                    }
                }

                if (showValidation && audioUri == null) {
                    item {
                        Text(
                            text = "Audio is required",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }


                if (audioUri != null) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = audioUri!!.lastPathSegment ?: "Audio selected",
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { audioUri = null }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove audio",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            Log.d("aa", imageUri?.path.toString())
                            Log.d("aa", audioUri?.path.toString())

                            showValidation = true
                            if (isFormValid) {
                                onConfirm(label, imageUri, audioUri)
                            } else {
                                coroutineScope.launch {
                                    val index =
                                        if (label.isBlank()) 0 else if (imageUri == null) 1 else 5
                                    listState.animateScrollToItem(index);
                                }
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Confirm",
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Scaffold {
                PositionIndicator(
                    scalingLazyListState = listState
                )
            }
        }
    }
}


suspend fun getConnectedPhoneNode(context: Context): String? {
    val nodes = Wearable.getNodeClient(context).connectedNodes.await()
    return nodes.find { it.isNearby }?.id
}

fun requestUpload(uploadType: String, context: Context, coroutineScope: CoroutineScope) {
    coroutineScope.launch {
        val phoneNodeId = getConnectedPhoneNode(context)

        if (phoneNodeId != null) {
            Wearable.getMessageClient(context).sendMessage(
                phoneNodeId,
                "/request_${uploadType}_upload",
                byteArrayOf()
            )
        }
    }
}

@Composable
fun VolumeButton(context: Context) {
    Button(
        onClick = {
            val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        },
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.volume_up),
                contentDescription = "Open volume control",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Volume",
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }
    }
}

@Composable
fun AddButton(buttonSize: Dp, onAdd: () -> Unit) {
    Button(
        onClick = onAdd,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB4D5FF)),
        modifier = Modifier
            .size(buttonSize)
            .padding(2.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.add),
                contentDescription = "Add sound",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Add Sound",
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        fontSize = 10.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AssetSelection(
    assetKey: String,
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    val assetUris = remember { mutableStateListOf<Uri>().apply {
        when (assetKey) {
            "image" -> addAll(getAllImageUris(context))
            "audio" -> addAll(getAllAudioUri(context))
        }
    } }
    val formattedAssetKey = assetKey.replaceFirstChar { it.titlecase() }

    val toast = Toast.makeText(context, "$formattedAssetKey Uploaded", Toast.LENGTH_SHORT)

    LaunchedEffect(Unit) {
        WearDataLayerListener.onImageReceived = { receivedUri ->
            Log.d("aa", "mega caller image")
            if (receivedUri !in assetUris) {
                assetUris.add(receivedUri)
                toast.show()
            }
        }

        WearDataLayerListener.onAudioReceived = { receivedUri ->
            Log.d("aa", "mega caller audio")
            if (receivedUri !in assetUris) {
                assetUris.add(receivedUri)
                toast.show()
            }
        }
    }

    ScalingLazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(10.dp)
    ) {
        item {
            Chip(
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(
                            painter = painterResource(id = R.drawable.phone),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Import $formattedAssetKey",
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                onClick = { requestUpload(assetKey, context, coroutineScope) },
                colors = ChipDefaults.chipColors(
                    backgroundColor = Color(0xFF4285F4),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(assetUris) { uri ->
            val fileName = uri.lastPathSegment ?: formattedAssetKey
            val fileSize = getFileSize(context, uri)

            Chip(
                label = {
                    Text(
                        text = fileName.take(20),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                secondaryLabel = {
                    Text(
                        text = fileSize,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                },
                icon = {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                },
                onClick = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selected${formattedAssetKey}}", uri)
                    navController.popBackStack()
                },
                colors = ChipDefaults.chipColors(
                    backgroundColor = Color.DarkGray,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Scaffold {
        PositionIndicator(
            scalingLazyListState = listState
        )
    }
}

fun getFileSize(context: Context, uri: Uri): String {
    return try {
        val file = File(uri.path ?: return "Unknown size")
        if (file.exists()) {
            val sizeInBytes = file.length()
            Formatter.formatShortFileSize(context, sizeInBytes)
        } else {
            // Try fallback using content resolver
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                val sizeInBytes = it.length
                Formatter.formatShortFileSize(context, sizeInBytes)
            } ?: "Unknown size"
        }
    } catch (e: Exception) {
        "Unknown size"
    }
}


fun getBundledAssetUris(context: Context, assetKey: String, names: List<String>): List<Uri> {
    val uriList = mutableListOf<Uri>()

    for (name in names) {
        if (assetKey == "image") {
            val resId = context.resources.getIdentifier(name.lowercase(), "drawable", context.packageName)
            if (resId != 0) {
                val uri = "android.resource://${context.packageName}/drawable/$name".toUri()
                uriList.add(uri)
            }
        } else if (assetKey == "audio") {
            val resId = context.resources.getIdentifier(name.lowercase(), "raw", context.packageName)
            if (resId != 0) {
                val uri = "android.resource://${context.packageName}/raw/$name".toUri()
                uriList.add(uri)
            }
        }
    }

    return uriList
}

fun getUploadedAssetUris(context: Context, fileName: String): List<Uri> {
    val assetDir = File(context.filesDir, fileName)
    return assetDir.listFiles()?.map { it.toUri() } ?: emptyList()
}

fun getAllImageUris(context: Context): List<Uri> {
    val bundled = getBundledAssetUris(context, "image", listOf("add_button"))
    val uploaded = getUploadedAssetUris(context, "images")
    return bundled + uploaded
}

fun getAllAudioUri(context: Context): List<Uri> {
    val bundled = getBundledAssetUris(context, "audio", listOf("wario"))
    val uploaded = getUploadedAssetUris(context, "audios")
    return bundled + uploaded
}


//@Composable
//fun VolumeController(context: Context) {
//    val audioManager = remember {
//        context.getSystemService(AUDIO_SERVICE) as AudioManager
//    }
//
//    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
//    var currentVolume by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
//
//    Column (
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(
//                top = 16.dp,
//                start = 16.dp,
//                end = 16.dp
//            ),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(
//            text = "Volume: " + (currentVolume / maxVolume * 100).toInt() + "%",
//            color = Color.White,
//        )
//
//        Slider(
//            value = currentVolume,
//            onValueChange = { newValue : Float ->
//                currentVolume = newValue
//
//                audioManager.setStreamVolume(
//                    AudioManager.STREAM_MUSIC,
//                    currentVolume.toInt(),
//                    0
//                )
//            },
//            steps = 100,
//            valueRange = 0f..maxVolume.toFloat(),
//            modifier = Modifier.fillMaxWidth(),
//            shape = CircleShape,
//        )
//    }
//}
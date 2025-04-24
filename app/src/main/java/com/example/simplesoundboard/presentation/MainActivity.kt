package com.example.simplesoundboard.presentation

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import com.example.simplesoundboard.R
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoundBoardScreen()
        }
    }
}

@Composable
fun SoundBoardScreen(itemsPerRow: Int = 3) {
    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester: FocusRequester = remember { FocusRequester() }

    val soundItems = remember { mutableStateListOf("ADD_BUTTON") }
    var soundCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ScalingLazyColumn (
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
                            rowItems.forEach { label ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(buttonSize)
                                ) {
                                    if (label == "ADD_BUTTON") {
                                        AddButton(buttonSize) {
                                            soundItems.add("Sound${soundCount++}")
                                        }
                                    } else {
                                        Button(
                                            onClick = { /* Handle click */ },
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .size(buttonSize)
                                                .padding(2.dp),
                                            contentPadding = PaddingValues(0.dp),
                                        ) {
                                            // Icon or symbol if you want
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = label,
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

@Composable
fun VolumeButton(context: Context) {
    Button(
        onClick = {
            val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4285F4)
        ),
        modifier = Modifier
            .width(150.dp)
            .height(50.dp)
            .padding(4.dp)
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
package app.mra.ktor.monitoring.interceptor.ui.component

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.mra.ktor.monitoring.interceptor.util.AudioCacheManager
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

private val PlayVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Play",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = addPathNodes("M8 5v14l11-7z"),
            fill = SolidColor(Color(0xFF2196F3))
        )
    }.build()
}

private val PauseVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Pause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = addPathNodes("M6 19h4V5H6v14zm8-14v14h4V5h-4z"),
            fill = SolidColor(Color(0xFF2196F3))
        )
    }.build()
}

@Composable
fun InlineAudioPlayerView(
    audioFilePath: String? = null,
    audioBytes: ByteArray? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(1f) }

    val mediaPlayer = remember { MediaPlayer() }
    var isPrepared by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            runCatching {
                if (mediaPlayer.isPlaying) {
                    currentPosition = mediaPlayer.currentPosition.toFloat().coerceIn(0f, duration)
                }
            }
            delay(50)
        }
    }

    val displaySizeKb = remember(audioFilePath, audioBytes) {
        when {
            audioFilePath != null -> (File(audioFilePath).length() / 1024).toInt()
            audioBytes != null -> audioBytes.size / 1024
            else -> 0
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    runCatching {
                        if (!isPrepared) {
                            val path = audioFilePath ?: audioBytes?.let { bytes ->
                                val info = AudioCacheManager.detectFileType(bytes)
                                AudioCacheManager.saveAudioBytes(context, bytes, info)
                            }
                            if (!path.isNullOrBlank()) {
                                mediaPlayer.reset()
                                mediaPlayer.setDataSource(path)
                                mediaPlayer.prepare()
                                duration = mediaPlayer.duration.coerceAtLeast(1).toFloat()
                                mediaPlayer.setOnCompletionListener {
                                    isPlaying = false
                                    currentPosition = duration
                                }
                                isPrepared = true
                            }
                        }

                        if (isPrepared) {
                            if (mediaPlayer.isPlaying) {
                                mediaPlayer.pause()
                                isPlaying = false
                            } else {
                                if (currentPosition >= duration) {
                                    mediaPlayer.seekTo(0)
                                    currentPosition = 0f
                                }
                                mediaPlayer.start()
                                isPlaying = true
                            }
                        }
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) PauseVectorIcon else PlayVectorIcon,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Audio Payload ($displaySizeKb KB)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = currentPosition.coerceIn(0f, duration),
                    onValueChange = { newPos ->
                        currentPosition = newPos
                        runCatching { mediaPlayer.seekTo(newPos.toInt()) }
                    },
                    valueRange = 0f..duration,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMs(currentPosition.toLong()),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = formatMs(duration.toLong()),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val seconds = ms / 1000
    val millis = (ms % 1000) / 100
    val minutes = seconds / 60
    val remSecs = seconds % 60
    return if (minutes > 0) {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, remSecs)
    } else {
        String.format(Locale.getDefault(), "00:%02d.%d", remSecs, millis)
    }
}

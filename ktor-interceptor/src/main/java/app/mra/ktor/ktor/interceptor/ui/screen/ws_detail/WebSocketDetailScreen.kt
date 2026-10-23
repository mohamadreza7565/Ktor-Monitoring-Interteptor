package app.mra.ktor.ktor.interceptor.ui.screen.ws_detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.mra.ktor.ktor.interceptor.model.FrameDirection
import app.mra.ktor.ktor.interceptor.model.FrameType
import app.mra.ktor.ktor.interceptor.model.NetworkLog
import app.mra.ktor.ktor.interceptor.model.WebSocketFrameLog
import app.mra.ktor.ktor.interceptor.store.InspectorLogStore
import app.mra.ktor.ktor.interceptor.ui.component.InlineAudioPlayerView
import app.mra.ktor.ktor.interceptor.util.AudioCacheManager
import okhttp3.internal.toImmutableList
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSocketDetailScreen(
    logId: String,
    onBack: () -> Unit
) {
    val logs by InspectorLogStore.logs.collectAsState()
    val wsLog = remember(logs, logId) { logs.firstOrNull { it.id == logId } as? NetworkLog.WebSocketLog }

    if (wsLog == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("WebSocket log not found")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WS - ${wsLog.statusCode ?: 101}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            contentDescription = null,
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status: ${wsLog.status.name}", fontWeight = FontWeight.Bold)
                        Text("Frames: ${wsLog.frames.size}", color = Color.Gray)
                    }
                    if (wsLog.closeReason != null) {
                        Text("Close Reason: ${wsLog.closeReason}", fontSize = 12.sp, color = Color.Red)
                    }
                }
            }

            Text(
                text = "Frame Event Stream",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            val sortedFrames = remember(wsLog.frames) {
                wsLog.frames.sortedByDescending { it.timestamp }.toImmutableList()
            }

            if (sortedFrames.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No frames exchanged yet", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedFrames, key = { it.id }) { frame ->
                        FrameItemCard(frame)
                    }
                }
            }
        }
    }
}

@Composable
private fun FrameItemCard(frame: WebSocketFrameLog) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val dirIcon = if (frame.direction == FrameDirection.SENT) "⬆️ OUT" else "⬇️ IN"
    val dirColor = if (frame.direction == FrameDirection.SENT) Color(0xFF2196F3) else Color(0xFF4CAF50)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = dirColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = dirIcon,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = frame.type.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = formatTime(frame.timestamp),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (frame.type) {
                FrameType.TEXT -> {
                    Text(
                        text = frame.textPayload ?: "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2
                    )
                }
                FrameType.BINARY -> {
                    Text(
                        text = if (frame.isAudio) "Binary Audio Payload (${frame.byteSize} bytes)" else "Binary File Payload (${frame.byteSize} bytes)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            if (frame.isAudio) {
                                InlineAudioPlayerView(
                                    audioFilePath = frame.cachedAudioPath,
                                    audioBytes = frame.binaryData
                                )
                            } else {
                                Button(
                                    onClick = {
                                        val path = frame.cachedAudioPath ?: frame.binaryData?.let { bytes ->
                                            val info = AudioCacheManager.detectFileType(bytes, frame.mimeType)
                                            AudioCacheManager.saveAudioBytes(context, bytes, info).also { saved ->
                                                frame.cachedAudioPath = saved
                                            }
                                        }
                                        if (path != null) {
                                            openFileWithIntent(context, path, frame.mimeType)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("📂 Open File (${frame.mimeType})")
                                }
                            }
                        }
                    }
                }
                else -> {
                    Text(text = "Control Frame", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

private fun openFileWithIntent(context: Context, filePath: String, mimeType: String) {
    runCatching {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.ktorinspector.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open File"))
    }.onFailure {
        runCatching {
            val file = File(filePath)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(file), mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

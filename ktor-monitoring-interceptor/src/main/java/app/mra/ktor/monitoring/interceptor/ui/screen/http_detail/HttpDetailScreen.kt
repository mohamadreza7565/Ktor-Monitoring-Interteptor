package app.mra.ktor.monitoring.interceptor.ui.screen.http_detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import app.mra.ktor.monitoring.interceptor.model.NetworkLog
import app.mra.ktor.monitoring.interceptor.store.InspectorLogStore
import app.mra.ktor.monitoring.interceptor.ui.component.InlineAudioPlayerView
import app.mra.ktor.monitoring.interceptor.util.AudioCacheManager
import app.mra.ktor.monitoring.interceptor.util.CurlFormatter
import app.mra.ktor.monitoring.interceptor.util.JsonFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpDetailScreen(
    logId: String,
    onBack: () -> Unit
) {
    val log = remember(logId) { InspectorLogStore.getLogById(logId) as? NetworkLog.HttpLog }
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    if (log == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Log not found")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${log.method} - ${log.statusCode ?: "Pending"}") },
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Request") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Headers") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Response") }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> RequestTab(log, context)
                    1 -> HeadersTab(log, context)
                    2 -> ResponseTab(log)
                }
            }
        }
    }
}

@Composable
private fun RequestTab(log: NetworkLog.HttpLog, context: Context) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = {
                val curl = CurlFormatter.format(log)
                copyToClipboard(context, "cURL Command", curl)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📋 Copy cURL Command")
        }

        DetailSection(title = "URL") {
            Text(text = log.url, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }

        if (log.queryParams.isNotEmpty()) {
            DetailSection(title = "Query Parameters") {
                log.queryParams.forEach { (key, values) ->
                    Text("$key = ${values.joinToString()}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }

        if (!log.requestBody.isNullOrBlank()) {
            DetailSection(title = "Request Body") {
                Text(
                    text = JsonFormatter.prettyPrint(log.requestBody),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun HeadersTab(log: NetworkLog.HttpLog, context: Context) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DetailSection(title = "Request Headers (${log.requestHeaders.size})") {
            if (log.requestHeaders.isEmpty()) {
                Text("No request headers", color = Color.Gray)
            } else {
                log.requestHeaders.forEach { (key, values) ->
                    HeaderRow(key, values.joinToString(), context)
                }
            }
        }

        DetailSection(title = "Response Headers (${log.responseHeaders.size})") {
            if (log.responseHeaders.isEmpty()) {
                Text("No response headers", color = Color.Gray)
            } else {
                log.responseHeaders.forEach { (key, values) ->
                    HeaderRow(key, values.joinToString(), context)
                }
            }
        }
    }
}

@Composable
private fun ResponseTab(log: NetworkLog.HttpLog) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DetailSection(title = "Status Metadata") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Code: ${log.statusCode ?: "Pending"}", fontWeight = FontWeight.Bold)
                Text("Duration: ${log.durationMs?.let { "${it} ms" } ?: "-"}")
                Text("Size: ${log.contentLength?.let { "${it / 1024} KB" } ?: "-"}")
            }
        }

        if (log.cachedAudioPath != null || AudioCacheManager.isAudioContentType(log.contentType)) {
            DetailSection(title = "Audio Player Response") {
                log.cachedAudioPath?.let { audioPath ->
                    InlineAudioPlayerView(audioFilePath = audioPath)
                } ?: Text("Audio payload available", color = Color.Gray)
            }
        }

        DetailSection(title = "Response Body") {
            if (log.responseBody.isNullOrBlank()) {
                Text("Empty response body", color = Color.Gray)
            } else {
                Text(
                    text = JsonFormatter.prettyPrint(log.responseBody),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(key: String, value: String, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = key, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.4f))
        Text(text = value, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
}

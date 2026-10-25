package app.mra.ktor.monitoring.interceptor.ui.screen.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mra.ktor.monitoring.interceptor.model.NetworkLog
import app.mra.ktor.monitoring.interceptor.store.InspectorLogStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestListScreen(
    onSelectHttpLog: (String) -> Unit,
    onSelectWsLog: (String) -> Unit
) {
    val logs by InspectorLogStore.logs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: ALL, 1: HTTP, 2: WS

    val filteredLogs = remember(logs, searchQuery, selectedFilter) {
        logs.filter { log ->
            val matchesSearch = log.url.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                1 -> log is NetworkLog.HttpLog
                2 -> log is NetworkLog.WebSocketLog
                else -> true
            }
            matchesSearch && matchesFilter
        }.sortedByDescending { it.timestamp }.toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ktor Inspector", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { InspectorLogStore.clearLogs() }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by URL...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    label = { Text("All (${logs.size})") }
                )
                FilterChip(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    label = { Text("HTTP (${logs.count { it is NetworkLog.HttpLog }})") }
                )
                FilterChip(
                    selected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 },
                    label = { Text("WS (${logs.count { it is NetworkLog.WebSocketLog }})") }
                )
            }

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No logs recorded yet", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        when (log) {
                            is NetworkLog.HttpLog -> HttpLogCard(log, onClick = { onSelectHttpLog(log.id) })
                            is NetworkLog.WebSocketLog -> WsLogCard(log, onClick = { onSelectWsLog(log.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HttpLogCard(log: NetworkLog.HttpLog, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(
                        text = log.method,
                        backgroundColor = getMethodColor(log.method)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (log.statusCode != null) {
                        Badge(
                            text = "${log.statusCode}",
                            backgroundColor = getStatusColor(log.statusCode)
                        )
                    }
                }
                Text(
                    text = formatTime(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.url,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.durationMs?.let { "${it} ms" } ?: "Pending...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = log.contentLength?.let { "${it / 1024} KB" } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun WsLogCard(log: NetworkLog.WebSocketLog, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(
                        text = "WS",
                        backgroundColor = Color(0xFF9C27B0)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        text = log.status.name,
                        backgroundColor = if (log.status.name == "CONNECTED") Color(0xFF4CAF50) else Color.Gray
                    )
                }
                Text(
                    text = formatTime(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.url,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Frames: ${log.frames.size}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun Badge(text: String, backgroundColor: Color) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun getMethodColor(method: String): Color {
    return when (method.uppercase()) {
        "GET" -> Color(0xFF2196F3)
        "POST" -> Color(0xFF4CAF50)
        "PUT" -> Color(0xFFFF9800)
        "DELETE" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

private fun getStatusColor(code: Int): Color {
    return when (code) {
        in 200..299 -> Color(0xFF4CAF50)
        in 300..399 -> Color(0xFFFF9800)
        in 400..599 -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

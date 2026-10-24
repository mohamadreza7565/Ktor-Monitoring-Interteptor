package app.mra.ktor.interceptor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mra.ktor.interceptor.ui.theme.KtorInterceptorTheme
import app.mra.ktor.monitoring.interceptor.plugin.KtorInspector
import app.mra.ktor.monitoring.interceptor.ui.InspectorActivity
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SamplePost(
    val title: String,
    val body: String,
    val userId: Int = 1
)

class MainActivity : ComponentActivity() {

    private val httpClient by lazy {
        HttpClient(OkHttp) {
            install(WebSockets)
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(KtorInspector) {
                enabled = true
                maxRetainedLogs = 100
                showNotification = true
                enableNotificationVibration = false
                enableNotificationSound = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KtorInterceptorTheme {
                MainScreen(httpClient = httpClient)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(httpClient: HttpClient) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf("Ready to send requests.") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ktor Interceptor Sample", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Open Inspector Button
            Button(
                onClick = {
                    val intent = Intent(context, InspectorActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "🚀 Launch Network Inspector UI",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "HTTP Requests Test",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // GET Request
            OutlinedButton(
                onClick = {
                    scope.launch {
                        statusText = "Sending GET request..."
                        try {
                            val response =
                                httpClient.get("https://jsonplaceholder.typicode.com/posts/1")
                            statusText = "GET Success (${response.status}):\n${
                                response.bodyAsText().take(150)
                            }..."
                        } catch (e: Exception) {
                            statusText = "GET Error: ${e.localizedMessage}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send GET Request (JSON)")
            }

            // POST Request
            OutlinedButton(
                onClick = {
                    scope.launch {
                        statusText = "Sending POST request..."
                        try {
                            val response =
                                httpClient.post("https://jsonplaceholder.typicode.com/posts") {
                                    contentType(ContentType.Application.Json)
                                    setBody(
                                        SamplePost(
                                            title = "Ktor Interceptor",
                                            body = "Testing network interceptor POST call"
                                        )
                                    )
                                }
                            statusText = "POST Success (${response.status}):\n${
                                response.bodyAsText().take(150)
                            }..."
                        } catch (e: Exception) {
                            statusText = "POST Error: ${e.localizedMessage}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send POST Request (JSON Body)")
            }

            // Media / Audio Request
            OutlinedButton(
                onClick = {
                    scope.launch {
                        statusText = "Fetching Audio Media file..."
                        try {
                            val response =
                                httpClient.get("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
                            statusText = "Audio Fetch Success (${response.status})"
                        } catch (e: Exception) {
                            statusText = "Audio Error: ${e.localizedMessage}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fetch Audio MP3 File")
            }

            // Error Request (404)
            OutlinedButton(
                onClick = {
                    scope.launch {
                        statusText = "Sending Request to 404 URL..."
                        try {
                            val response =
                                httpClient.get("https://jsonplaceholder.typicode.com/non-existing-page-404")
                            statusText = "Request Result (${response.status})"
                        } catch (e: Exception) {
                            statusText = "Request Error: ${e.localizedMessage}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send 404 Error Request")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "WebSocket Test",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // WebSocket Session
            OutlinedButton(
                onClick = {
                    scope.launch {
                        statusText = "Connecting to WebSocket..."
                        try {
                            httpClient.webSocket("wss://ws.postman-echo.com/raw") {
                                statusText = "WebSocket Connected! Sending Frame..."
                                send(Frame.Text("Hello from Ktor Interceptor!"))
                                for (frame in incoming) {
                                    if (frame is Frame.Text) {
                                        statusText = "WS Frame Received: ${frame.readText()}"
                                        break
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            statusText = "WebSocket Error: ${e.localizedMessage}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test WebSocket Connection & Frames")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Response Status Log:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(statusText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

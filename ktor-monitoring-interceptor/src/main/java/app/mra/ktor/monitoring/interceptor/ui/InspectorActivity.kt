package app.mra.ktor.monitoring.interceptor.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import app.mra.ktor.monitoring.interceptor.ui.navigation.InspectorNavGraph

class InspectorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    InspectorNavGraph()
                }
            }
        }
    }
}

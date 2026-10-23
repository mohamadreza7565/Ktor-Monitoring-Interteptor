package app.mra.ktor.ktor.interceptor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.mra.ktor.ktor.interceptor.ui.screen.http_detail.HttpDetailScreen
import app.mra.ktor.ktor.interceptor.ui.screen.list.RequestListScreen
import app.mra.ktor.ktor.interceptor.ui.screen.ws_detail.WebSocketDetailScreen

@Composable
fun InspectorNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            RequestListScreen(
                onSelectHttpLog = { logId -> navController.navigate("http_detail/$logId") },
                onSelectWsLog = { logId -> navController.navigate("ws_detail/$logId") }
            )
        }

        composable(
            route = "http_detail/{logId}",
            arguments = listOf(navArgument("logId") { type = NavType.StringType })
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getString("logId") ?: ""
            HttpDetailScreen(
                logId = logId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "ws_detail/{logId}",
            arguments = listOf(navArgument("logId") { type = NavType.StringType })
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getString("logId") ?: ""
            WebSocketDetailScreen(
                logId = logId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

package no.secret24h.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "feed") {
        composable("feed") {
            MainScreen(navController = navController)
        }
        composable("auth") {
            AuthScreen(
                onSuccess = { navController.popBackStack() },
                onSkip = { navController.popBackStack() },
            )
        }
        composable("my-secrets") {
            MySecretsScreen(onBack = { navController.popBackStack() })
        }
        composable("inbox") {
            InboxScreen(
                onBack = { navController.popBackStack() },
                onConversation = { secretId, otherUserId ->
                    navController.navigate("whisper/$secretId/$otherUserId")
                },
            )
        }
        composable(
            "whisper/{secretId}/{receiverId}",
            arguments = listOf(
                navArgument("secretId") { type = NavType.StringType },
                navArgument("receiverId") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val secretId = backStackEntry.arguments?.getString("secretId") ?: ""
            val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
            WhisperScreen(
                secretId = secretId,
                otherUserId = receiverId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

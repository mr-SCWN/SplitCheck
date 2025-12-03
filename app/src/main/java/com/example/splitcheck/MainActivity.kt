package com.example.splitcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.splitcheck.ui.HomeScreen
import com.example.splitcheck.ui.PhotoPreviewScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(navController)
        }

        composable(
            route = "preview?uri={uri}",
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { entry ->
            val uri = entry.arguments?.getString("uri")
            PhotoPreviewScreen(uri)
        }
    }
}

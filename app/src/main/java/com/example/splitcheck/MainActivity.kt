package com.example.splitcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.splitcheck.ui.HomeScreen
import com.example.splitcheck.ui.PhotoPreviewScreen
import com.example.splitcheck.ui.ProductListScreen
import com.example.splitcheck.ui.SummaryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigation()
            }
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
            PhotoPreviewScreen(uri, navController)
        }

        composable(
            route = "products?uri={uri}&people={people}",
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("people") { type = NavType.IntType }
            )
        ) { entry ->
            val uri = entry.arguments?.getString("uri")
            val people = entry.arguments?.getInt("people") ?: 1
            ProductListScreen(uri = uri, people = people, navController = navController)
        }

        composable("summary") {
            SummaryScreen(navController = navController)
        }
    }
}

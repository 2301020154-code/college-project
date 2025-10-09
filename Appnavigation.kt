package com.example.trackback

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val uploadViewModel: UploadViewModel = viewModel()
    val startDestination = if (Firebase.auth.currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("signup") {
            SignUpScreen(navController = navController)
        }
        composable("home") {
            HomeScreen(navController = navController)
        }
        // Add the new route for the profile screen
        composable("profile") {
            ProfileScreen(navController = navController)
        }
        composable("upload_item") {
            UploadItemScreen(navController = navController, uploadViewModel = uploadViewModel)
        }
        composable("location_picker") {
            LocationPickerScreen(navController = navController)
        }
    }
}
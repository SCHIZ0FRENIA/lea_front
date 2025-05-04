package by.andros.lea_front

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import by.andros.lea_front.app.presentation.home.HomeScreen
import by.andros.lea_front.auth.presentation.login.LoginScreen
import by.andros.lea_front.auth.presentation.registration.RegistrationScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onNavigateToRegistration = { navController.navigate("registration") },
                onNavigateToHome = { navController.navigate("home") }
            )
        }
        
        composable("registration") {
            RegistrationScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToHome = { navController. navigate("home") }
            )
        }
        
        composable("home") {
            HomeScreen()
        }
    }
}
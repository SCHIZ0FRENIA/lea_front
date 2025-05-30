package by.andros.lea_front

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import by.andros.lea_front.app.presentation.admin.AdminScreen
import by.andros.lea_front.app.presentation.home.HomeScreen
import by.andros.lea_front.app.presentation.home.cards.AllCardsScreen
import by.andros.lea_front.app.presentation.home.decks.AllDecksScreen
import by.andros.lea_front.app.presentation.publicdecks.PublicDecksScreen
import by.andros.lea_front.auth.presentation.login.LoginScreen
import by.andros.lea_front.auth.presentation.registration.RegistrationScreen
import by.andros.lea_front.splash.SplashScreen
import by.andros.lea_front.app.presentation.profile.ProfileScreen
import by.andros.lea_front.theme.PREFS_NAME

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    // Check if user is admin
                    val userRole = sharedPreferences.getString("role", null)
                    if (userRole == "admin") {
                        navController.navigate("admin") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                onNavigateToRegistration = { navController.navigate("registration") },
                onNavigateToHome = {
                    // Check if user is admin after login
                    val userRole = sharedPreferences.getString("role", null)
                    if (userRole == "admin") {
                        navController.navigate("admin") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("registration") {
            RegistrationScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToHome = { 
                    // Check if user is admin after registration
                    val userRole = sharedPreferences.getString("role", null)
                    if (userRole == "admin") {
                        navController.navigate("admin") {
                            popUpTo("registration") { inclusive = true }
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("home") {
                            popUpTo("registration") { inclusive = true }
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("admin") {
            AdminScreen(
                onLogout = {
                    // Clear auth data
                    sharedPreferences.edit().remove("jwt").remove("role").apply()
                    navController.navigate("login") {
                        popUpTo("admin") { inclusive = true }
                    }
                },
                onNavigateToPublicDecks = {
                    navController.navigate("public_decks_admin")
                }
            )
        }

        // Add a special public decks route for admins with full management capabilities
        composable("public_decks_admin") {
            PublicDecksScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Regular public decks route for normal users
        composable("public_decks") {
            PublicDecksScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("home") {
            // Check if user is admin, if yes redirect to admin screen
            LaunchedEffect(Unit) {
                val userRole = sharedPreferences.getString("role", null)
                if (userRole == "admin") {
                    navController.navigate("admin") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            }
            
            HomeScreen(
                onNavigateToShowAllDecks = { navController.navigate("all_decks") },
                onNavigateToShowCards = { deckId -> navController.navigate("all_cards/$deckId") },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToPublicDecks = { navController.navigate("public_decks") }
            )
        }

        composable("all_decks") {
            AllDecksScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToShowCards = { deckId -> navController.navigate("all_cards/$deckId") }
            )
        }

        composable(
            route = "all_cards/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
        ) {
            val deckId = it.arguments?.getLong("deckId")
            if (deckId != null) {
                AllCardsScreen(
                    deckId = deckId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCardDetail = { cardId ->
                        // For now, we'll just navigate back as there's no card detail screen
                        // In a complete implementation, you would navigate to the card detail screen
                        navController.navigate("card_detail/$cardId")
                    }
                )
            } else {
            }
        }

        composable("profile") {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

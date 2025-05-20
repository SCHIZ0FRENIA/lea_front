package by.andros.lea_front

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import by.andros.lea_front.app.presentation.home.HomeScreen
import by.andros.lea_front.app.presentation.home.cards.AllCardsScreen
import by.andros.lea_front.app.presentation.home.decks.AllDecksScreen
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
            HomeScreen(
                onNavigateToShowAllDecks = { navController.navigate("all_decks") },
                onNavigateToShowCards = { deckId -> navController.navigate("all_cards/$deckId") },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
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
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
            }
        }
    }
}

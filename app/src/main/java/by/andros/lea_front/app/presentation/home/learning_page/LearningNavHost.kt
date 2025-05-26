package by.andros.lea_front.app.presentation.home.learning_page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import by.andros.lea_front.learning.LearningUiState
import by.andros.lea_front.learning.LearningUserEvent
import by.andros.lea_front.learning.LearningViewModel

@Composable
fun LearningNavHost(
    // It's often better to have the NavController created and managed by the main app navigation
    // and passed down if this LearningNavHost is part of a larger navigation graph.
    // However, for a self-contained learning module, creating it here is also fine.
    navController: NavHostController = rememberNavController(),
    onExitLearningFlow: () -> Unit // Callback to exit the entire learning module (e.g., back to Home)
) {
    val viewModel: LearningViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = "learning/select") {
        composable("learning/select") {
            LearningScreen(
                viewModel = viewModel,
                onStartSession = {
                    navController.navigate("learning/session") {}
                }
            )
        }
        composable("learning/session") {
            LearningSessionScreen(
                viewModel = viewModel,
                onFinish = { // Called from session screen when user exits or session naturally ends
                    // ViewModel state will be SessionFinished or Idle (if exited early)
                    // Navigation is handled by observing uiState below
                }
            )
        }
        composable("learning/stats") {
             // Ensure we are in the correct state to show stats
            val currentUiState = viewModel.uiState.value
            if (currentUiState is LearningUiState.SessionFinished) {
                LearningStatsScreen(
                    stats = currentUiState.stats,
                    onBackToHome = {
                        viewModel.onEvent(LearningUserEvent.ReturnToDeckSelection)
                        // Navigation back to select is handled by uiState change
                    }
                )
            } else {
                // If not in finished state, perhaps navigate back to selection or log error
                LaunchedEffect(Unit) {
                    navController.popBackStack("learning/select", inclusive = false)
                }
            }
        }
    }

    // Centralized navigation logic based on ViewModel's UiState changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is LearningUiState.Idle -> {
                // If current route is not deck selection, navigate there.
                // This handles finishing a session or exiting early.
                if (navController.currentDestination?.route != "learning/select") {
                    navController.navigate("learning/select") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
            is LearningUiState.SessionFinished -> {
                if (navController.currentDestination?.route != "learning/stats") {
                    navController.navigate("learning/stats") {
                        popUpTo("learning/select") // Keep select in backstack for stats screen's back nav
                    }
                }
            }
            // Add other state-driven navigation if needed, e.g., exiting the whole flow
            else -> {
                // No universal navigation action for other states like DeckSelection, InProgress, Paused, Error
                // as they are typically handled by their respective screens or user actions within them.
            }
        }
    }
     // Example of how onExitLearningFlow might be triggered from a button within one of the screens,
     // usually via a ViewModel event that the NavHost then observes.
     // For now, it's a passed lambda that specific screens could call if needed.
} 
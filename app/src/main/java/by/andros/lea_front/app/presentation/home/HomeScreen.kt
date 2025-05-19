@file:OptIn(ExperimentalMaterial3Api::class)

package by.andros.lea_front.app.presentation.home

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import by.andros.lea_front.app.presentation.home.home_page.HomePage
import by.andros.lea_front.app.presentation.home.learning_page.LearningPage
import by.andros.lea_front.app.presentation.home.cards.AllCardsScreen
import androidx.navigation.navArgument
import androidx.navigation.NavType


@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToShowAllDecks: () -> Unit,
    onNavigateToShowCards: (Long) -> Unit
) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            entry.destination.route?.let { route ->
                if (route == Screen.Home.route || route == Screen.Learning.route) {
                    viewModel.onEvent(HomeScreenEvent.SyncRoute(route))
                }
            }
        }
    }

    LaunchedEffect(state.currentRoute) {
        val currentDestination = navController.currentDestination?.route
        if (state.currentRoute != currentDestination && (state.currentRoute == Screen.Home.route || state.currentRoute == Screen.Learning.route)) {
            navController.navigate(state.currentRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold (
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = 24.dp),
                title = {
                    Text(
                        text = "LEA",
                        fontSize = 30.sp,
                        letterSpacing = 0.5.sp,
                    )
                },
                actions = {
                    TextButton(
                        onClick = {}
                    ) {
                        Text(
                            text = "User",
                            fontSize = 18.sp,
                            letterSpacing = 0.5.sp,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                listOf(Screen.Home, Screen.Learning).forEach { screen ->
                    Log.d("asdf", screen.route)
                    NavigationBarItem(
                        icon = {
                            when (screen) {
                                is Screen.Home -> Icon(Icons.Default.Home, contentDescription = screen.route)
                                is Screen.Learning -> Icon(Icons.Default.Book, contentDescription = screen.route)
                            }
                        },
                        label = { Text(screen.route) },
                        selected = state.currentRoute == screen.route,
                        onClick = {
                            viewModel.onEvent(
                                when (screen) {
                                    is Screen.Home -> HomeScreenEvent.NavigateToHome
                                    is Screen.Learning -> HomeScreenEvent.NavigateToLearning
                                }
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xff2a4174),
                            selectedTextColor = Color(0xff2a4174),
                            indicatorColor = Color.Transparent,
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = paddingValues.calculateBottomPadding(),
                    top = paddingValues.calculateTopPadding(),
                )
                .padding(horizontal = 48.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Screen.Home.route) {
                    HomePage(
                        onNavigateToShowAllDecks = onNavigateToShowAllDecks,
                        onNavigateToShowCards = onNavigateToShowCards
                    )
                }
                composable(Screen.Learning.route) { LearningPage() }

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
    }
}

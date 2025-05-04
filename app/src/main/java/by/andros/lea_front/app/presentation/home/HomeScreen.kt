@file:OptIn(ExperimentalMaterial3Api::class)

package by.andros.lea_front.app.presentation.home

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import by.andros.lea_front.app.presentation.home.home_page.HomePage
import by.andros.lea_front.app.presentation.home.learning_page.LearningPage

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsState()
    
    Scaffold (
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = 24.dp),
                title = {
                    Text(
                        text = "Title",
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
                val currentRoute = state.currentRoute
                listOf(Screen.Home, Screen.Learning).forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            when (screen) {
                                is Screen.Home -> Icon(Icons.Default.Home, "")
                                is Screen.Learning -> Icon(Icons.Default.Book, "")
                            }
                        },
                        label = { Text(screen.route) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            viewModel.onEvent(
                                when (screen) {
                                    is Screen.Home -> HomeScreenEvent.NavigateToHome
                                    is Screen.Learning -> HomeScreenEvent.NavigateToLearning
                                }
                            )
                        }
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
                composable(Screen.Home.route) { HomePage() }
                composable(Screen.Learning.route) { LearningPage() }
            }
            
        }
    }
}
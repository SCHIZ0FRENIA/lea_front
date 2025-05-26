package by.andros.lea_front.app.presentation.home.learning_page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import by.andros.lea_front.learning.LearningUiState
import by.andros.lea_front.learning.LearningUserEvent
import by.andros.lea_front.learning.LearningViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LearningSessionScreen(
    viewModel: LearningViewModel = hiltViewModel(),
    onFinish: () -> Unit // This callback is now less critical as NavHost handles state-based nav
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val currentState = uiState) {
        is LearningUiState.InProgress -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(currentState.progressText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    AnimatedContent(
                        targetState = currentState.showAnswer,
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                    ) { showAnswer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(200.dp),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (showAnswer) currentState.currentCard.back else currentState.currentCard.front,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    if (!currentState.showAnswer) {
                        Button(onClick = { viewModel.onEvent(LearningUserEvent.ShowCardAnswer) }) {
                            Text("Show Answer")
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            (1..5).forEach { rating ->
                                Button(
                                    onClick = { viewModel.onEvent(LearningUserEvent.SubmitCardAnswer(rating)) },
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                ) { Text(rating.toString()) }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row {
                        IconButton(onClick = { viewModel.onEvent(LearningUserEvent.PauseSession) }) {
                            Icon(Icons.Filled.Pause, contentDescription = "Pause")
                        }
                        Spacer(Modifier.width(16.dp))
                        IconButton(onClick = { viewModel.onEvent(LearningUserEvent.EndSessionAndExit) }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Exit")
                        }
                    }
                }
            }
        }
        is LearningUiState.Paused -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Session Paused", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.onEvent(LearningUserEvent.ResumeSession) }) { Text("Resume") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.onEvent(LearningUserEvent.EndSessionAndExit) }) { Text("Exit Session") }
                }
            }
        }
        is LearningUiState.Error -> {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(currentState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        // SessionFinished and Idle states should trigger navigation via LearningNavHost
        // based on LaunchedEffect observing uiState.
        else -> {
            // If this screen is shown for an unexpected state, show loading or an error.
            // LearningNavHost should ideally prevent this.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator() 
            }
        }
    }
} 
package by.andros.lea_front.app.presentation.home.learning_page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import by.andros.lea_front.app.data.Deck
import by.andros.lea_front.learning.LearningUiState
import by.andros.lea_front.learning.LearningUserEvent
import by.andros.lea_front.learning.LearningViewModel

@Composable
fun LearningScreen(
    viewModel: LearningViewModel = hiltViewModel(),
    onStartSession: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(LearningUserEvent.RefreshDeckData)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val currentState = uiState
    when (currentState) {
        is LearningUiState.DeckSelection -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Select Decks to Study", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                if (currentState.allDecksWithCount.isEmpty()) {
                    Text("No decks available. Go to 'All Decks' to create some!")
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            currentState.allDecksWithCount.filter { it.deck.id != null }.forEach { deckWithCount ->
                                DeckRow(
                                    deck = deckWithCount.deck,
                                    isSelected = deckWithCount.deck.id!! in currentState.selectedDeckIds,
                                    isDeckEmpty = deckWithCount.cardCount == 0,
                                    onToggleSelection = {
                                        deckWithCount.deck.id.let {
                                            viewModel.onEvent(LearningUserEvent.ToggleDeckSelection(it))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                IconButton(
                    onClick = {
                        viewModel.onEvent(LearningUserEvent.StartLearningSession)
                    },
                    enabled = currentState.selectedDeckIds.isNotEmpty(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Start Learning")
                }
            }
        }
        is LearningUiState.Idle -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is LearningUiState.InProgress -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is LearningUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(currentState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("STATE: Other/Unhandled - $currentState", fontSize = 18.sp)
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is LearningUiState.InProgress) {
            onStartSession()
        }
    }
}

@Composable
fun DeckRow(
    deck: Deck,
    isSelected: Boolean,
    isDeckEmpty: Boolean,
    onToggleSelection: () -> Unit
) {
    val displayName = if (isDeckEmpty) "${deck.name} - Empty" else deck.name
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { if (!isDeckEmpty) onToggleSelection() },
            enabled = !isDeckEmpty
        )
        Spacer(Modifier.width(8.dp))
        Text(text = displayName, style = MaterialTheme.typography.bodyLarge)
    }
} 
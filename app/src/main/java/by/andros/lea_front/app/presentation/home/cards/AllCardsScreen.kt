package by.andros.lea_front.app.presentation.home.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.andros.lea_front.app.data.Card
import by.andros.lea_front.app.data.CardRepository
import by.andros.lea_front.utils.getDaysSinceEpoch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AllCardsState {
    data object Loading : AllCardsState()
    data class Loaded(val cards: List<Card>) : AllCardsState()
    data class Error(val message: String) : AllCardsState()
}

sealed class AllCardsEvent {
    data object LoadCards : AllCardsEvent()
    data class AddCard(val front: String, val back: String) : AllCardsEvent()
}

@HiltViewModel
class AllCardsViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deckId: Long = savedStateHandle.get<Long>("deckId")
        ?: throw IllegalArgumentException("deckId is required")

    private val _state = MutableStateFlow<AllCardsState>(AllCardsState.Loading)
    val state: StateFlow<AllCardsState> = _state

    init {
        onEvent(AllCardsEvent.LoadCards)
    }

    fun onEvent(event: AllCardsEvent) {
        when (event) {
            is AllCardsEvent.LoadCards -> loadCards()
            is AllCardsEvent.AddCard -> addCard(event.front, event.back)
        }
    }

    private fun loadCards() {
        viewModelScope.launch {
            _state.value = AllCardsState.Loading
            cardRepository.getCardsByDeck(deckId)
                .catch { e ->
                    _state.value = AllCardsState.Error("Error loading cards: ${e.message}")
                }
                .collect { cards ->
                    _state.value = AllCardsState.Loaded(cards)
                }
        }
    }

    private fun addCard(front: String, back: String) {
        viewModelScope.launch {
            val newCard = Card(
                front = front,
                back = back,
                lastReview = getDaysSinceEpoch().toInt(),
                nextReview = getDaysSinceEpoch().toInt(),
                deckId = deckId
            )
            try {
                cardRepository.insertCard(newCard)
                loadCards()
            } catch (e: Exception) {
                _state.value = AllCardsState.Error("Error adding card: ${e.message}")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCardsScreen(
    viewModel: AllCardsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showNewCardDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cards") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewCardDialog = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add new card")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            when (val currentState = state) {
                is AllCardsState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is AllCardsState.Loaded -> {
                    if (currentState.cards.isEmpty()) {
                        Text("No cards found in this deck.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentState.cards) { card ->
                                CardItem(card = card) {
                                }
                            }
                        }
                    }
                }

                is AllCardsState.Error -> {
                    Text("Error: ${currentState.message}")
                }
            }
        }
    }

    if (showNewCardDialog) {
        NewCardDialog(
            onDismiss = { showNewCardDialog = false },
            onCardCreated = { front, back ->
                viewModel.onEvent(AllCardsEvent.AddCard(front, back))
                showNewCardDialog = false
            }
        )
    }
}

@Composable
fun CardItem(
    card: Card,
    onCardClick: (Long?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick(card.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Front: ${card.front}")
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Back: ${card.back}")
        }
    }
}

@Composable
fun NewCardDialog(
    onDismiss: () -> Unit,
    onCardCreated: (String, String) -> Unit
) {
    var cardFront by remember { mutableStateOf("") }
    var cardBack by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Card") },
        text = {
            Column {
                OutlinedTextField(
                    value = cardFront,
                    onValueChange = { cardFront = it },
                    label = { Text("Front") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cardBack,
                    onValueChange = { cardBack = it },
                    label = { Text("Back") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (cardFront.isNotBlank() && cardBack.isNotBlank()) {
                        onCardCreated(cardFront, cardBack)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

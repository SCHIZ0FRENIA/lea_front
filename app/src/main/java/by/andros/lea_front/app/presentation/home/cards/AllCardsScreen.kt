package by.andros.lea_front.app.presentation.home.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
import by.andros.lea_front.app.data.DeckRepository
import by.andros.lea_front.app.data.Deck
import by.andros.lea_front.auth.domain.AuthRepository

sealed class AllCardsState {
    data object Loading : AllCardsState()
    data class Loaded(
        val cards: List<Card>,
        val deck: Deck? = null,
        val isUserLoggedIn: Boolean = false,
        val showDeleteConfirmation: Boolean = false,
        val showEditDeckDialog: Boolean = false,
        val navigationEvents: AllCardsNavigationEvent? = null // For navigation after delete
    ) : AllCardsState()
    data class Error(val message: String) : AllCardsState()
}

sealed class AllCardsEvent {
    data object LoadData : AllCardsEvent() // Renamed from LoadCards for clarity
    data class AddCard(val front: String, val back: String) : AllCardsEvent()
    data class UpdateDeck(val name: String, val description: String?) : AllCardsEvent()
    data object DeleteDeck : AllCardsEvent()
    data object ConfirmDeleteDeck : AllCardsEvent()
    data object CancelDeleteDeck : AllCardsEvent()
    data object ShowEditDeckDialog : AllCardsEvent()
    data object HideEditDeckDialog : AllCardsEvent()
    data object PublishDeck : AllCardsEvent() // Placeholder
    data object ClearNavigationEvent : AllCardsEvent()
    data class EditCard(val card: Card, val newFront: String, val newBack: String) : AllCardsEvent()
    data class DeleteCard(val card: Card) : AllCardsEvent()
}

sealed class AllCardsNavigationEvent {
    data object NavigateBack : AllCardsNavigationEvent()
}

@HiltViewModel
class AllCardsViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository, // Added DeckRepository
    private val authRepository: AuthRepository, // Added AuthRepository
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deckId: Long = savedStateHandle.get<Long>("deckId")
        ?: throw IllegalArgumentException("deckId is required")

    private val _state = MutableStateFlow<AllCardsState>(AllCardsState.Loading)
    val state: StateFlow<AllCardsState> = _state.asStateFlow()

    init {
        onEvent(AllCardsEvent.LoadData)
    }

    fun onEvent(event: AllCardsEvent) {
        when (event) {
            is AllCardsEvent.LoadData -> loadAllData()
            is AllCardsEvent.AddCard -> addCard(event.front, event.back)
            is AllCardsEvent.UpdateDeck -> updateDeck(event.name, event.description)
            is AllCardsEvent.DeleteDeck -> showDeleteConfirmation()
            is AllCardsEvent.ConfirmDeleteDeck -> deleteDeckAndNavigate()
            is AllCardsEvent.CancelDeleteDeck -> hideDeleteConfirmation()
            is AllCardsEvent.ShowEditDeckDialog -> showEditDeckDialog()
            is AllCardsEvent.HideEditDeckDialog -> hideEditDeckDialog()
            is AllCardsEvent.PublishDeck -> publishDeck()
            is AllCardsEvent.ClearNavigationEvent -> clearNavigationEvent()
            is AllCardsEvent.EditCard -> editCard(event.card, event.newFront, event.newBack)
            is AllCardsEvent.DeleteCard -> deleteCard(event.card)
        }
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _state.value = AllCardsState.Loading
            try {
                // Combine flows for cards, deck details, and login status
                combine(
                    cardRepository.getCardsByDeck(deckId),
                    deckRepository.getDeckById(deckId),
                    isUserLoggedInFlow() // Replaced with a flow from AuthRepository
                ) { cards, deck, isLoggedIn ->
                    AllCardsState.Loaded(cards = cards, deck = deck, isUserLoggedIn = isLoggedIn)
                }.catch { e ->
                    _state.value = AllCardsState.Error("Error loading data: ${e.message}")
                }.collect { loadedState ->
                    _state.value = loadedState
                }
            } catch (e: Exception) {
                 _state.value = AllCardsState.Error("Error initializing data load: ${e.message}")
            }
        }
    }
    
    // Helper function to get login status as a Flow
    private fun isUserLoggedInFlow(): Flow<Boolean> = flow {
        // Assuming getString returns null if "jwt" is not found
        emit(authRepository.getJwtToken() != null)
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
                // Reload all data to refresh the state including cards
                loadAllData()
            } catch (e: Exception) {
                _state.value = AllCardsState.Error("Error adding card: ${e.message}")
            }
        }
    }

    private fun updateDeck(name: String, description: String?) {
        viewModelScope.launch {
            val currentDeck = (_state.value as? AllCardsState.Loaded)?.deck
            if (currentDeck != null) {
                val updatedDeck = currentDeck.copy(name = name, description = description)
                try {
                    deckRepository.updateDeck(updatedDeck)
                    // Reload all data to refresh the state including deck details
                    loadAllData()
                    hideEditDeckDialog() // Hide dialog on success
                } catch (e: Exception) {
                    _state.value = AllCardsState.Error("Error updating deck: ${e.message}")
                }
            }
        }
    }

    private fun showDeleteConfirmation() {
        (_state.value as? AllCardsState.Loaded)?.let {
            _state.value = it.copy(showDeleteConfirmation = true)
        }
    }

    private fun hideDeleteConfirmation() {
        (_state.value as? AllCardsState.Loaded)?.let {
            _state.value = it.copy(showDeleteConfirmation = false)
        }
    }

    private fun deleteDeckAndNavigate() {
        viewModelScope.launch {
            val currentDeck = (_state.value as? AllCardsState.Loaded)?.deck
            if (currentDeck != null) {
                try {
                    deckRepository.deleteDeck(currentDeck)
                    // Navigate back after deletion
                     _state.update { currentState ->
                        if (currentState is AllCardsState.Loaded) {
                            currentState.copy(navigationEvents = AllCardsNavigationEvent.NavigateBack, showDeleteConfirmation = false)
                        } else {
                            currentState
                        }
                    }
                } catch (e: Exception) {
                    _state.value = AllCardsState.Error("Error deleting deck: ${e.message}")
                }
            }
        }
    }

    private fun showEditDeckDialog() {
        (_state.value as? AllCardsState.Loaded)?.let {
            _state.value = it.copy(showEditDeckDialog = true)
        }
    }

    private fun hideEditDeckDialog() {
        (_state.value as? AllCardsState.Loaded)?.let {
            _state.value = it.copy(showEditDeckDialog = false)
        }
    }
    
    private fun clearNavigationEvent() {
        _state.update { currentState ->
            if (currentState is AllCardsState.Loaded) {
                currentState.copy(navigationEvents = null)
            } else {
                currentState
            }
        }
    }

    private fun publishDeck() {
        // Placeholder for publish logic
        viewModelScope.launch {
            // Implement publish logic here, e.g., call a repository method
            // For now, just update state to indicate action if necessary or log
            // _state.value = AllCardsState.Error("Publish function not implemented yet.")
        }
    }

    private fun editCard(card: Card, newFront: String, newBack: String) {
        viewModelScope.launch {
            val updatedCard = Card(
                id = card.id,
                front = newFront,
                back = newBack,
                repetitions = card.repetitions,
                interval = card.interval,
                lastReview = card.lastReview,
                nextReview = card.nextReview,
                easeFactor = card.easeFactor,
                deckId = card.deckId,
                createdAt = card.createdAt
            )
            try {
                cardRepository.updateCard(updatedCard)
                // Reload all data to refresh the state including cards
                loadAllData()
            } catch (e: Exception) {
                _state.value = AllCardsState.Error("Error editing card: ${e.message}")
            }
        }
    }

    private fun deleteCard(card: Card) {
        viewModelScope.launch {
            try {
                cardRepository.deleteCard(card)
                // Reload all data to refresh the state including cards
                loadAllData()
            } catch (e: Exception) {
                _state.value = AllCardsState.Error("Error deleting card: ${e.message}")
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
    val stateFlow = viewModel.state
    val state by stateFlow.collectAsState()
    var showNewCardDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<Card?>(null) }
    var cardToDelete by remember { mutableStateOf<Card?>(null) }

    // Handle navigation events
    LaunchedEffect(state) {
        if (state is AllCardsState.Loaded) {
            val loadedState = state as AllCardsState.Loaded
            if (loadedState.navigationEvents is AllCardsNavigationEvent.NavigateBack) {
                onNavigateBack()
                viewModel.onEvent(AllCardsEvent.ClearNavigationEvent) // Reset event
            }
        }
    }

    Scaffold(
        topBar = {
            val currentDeck = (state as? AllCardsState.Loaded)?.deck
            val isUserLoggedIn = (state as? AllCardsState.Loaded)?.isUserLoggedIn ?: false

            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentDeck?.name ?: "Deck Details",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!currentDeck?.description.isNullOrBlank()) {
                            Text(
                                text = currentDeck?.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Edit Button
                    IconButton(onClick = { viewModel.onEvent(AllCardsEvent.ShowEditDeckDialog) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Deck")
                    }
                    // Delete Button
                    IconButton(onClick = { viewModel.onEvent(AllCardsEvent.DeleteDeck) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Deck")
                    }
                    // Publish Button (conditionally displayed)
                    if (isUserLoggedIn) {
                        IconButton(onClick = { viewModel.onEvent(AllCardsEvent.PublishDeck) }) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = "Publish Deck")
                        }
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
                                CardItem(
                                    card = card,
                                    onCardClick = {},
                                    onEditClick = { cardToEdit = card },
                                    onDeleteClick = { cardToDelete = card }
                                )
                            }
                        }
                    }

                    // Edit Deck Dialog
                    if (currentState.showEditDeckDialog) {
                        EditDeckDialog(
                            deck = currentState.deck,
                            onDismiss = { viewModel.onEvent(AllCardsEvent.HideEditDeckDialog) },
                            onConfirm = { name, description ->
                                viewModel.onEvent(AllCardsEvent.UpdateDeck(name, description))
                            }
                        )
                    }

                    // Delete Confirmation Dialog
                    if (currentState.showDeleteConfirmation) {
                        DeleteConfirmDialog(
                            deckName = currentState.deck?.name ?: "this deck",
                            onDismiss = { viewModel.onEvent(AllCardsEvent.CancelDeleteDeck) },
                            onConfirm = { viewModel.onEvent(AllCardsEvent.ConfirmDeleteDeck) }
                        )
                    }

                    // Edit Card Dialog
                    cardToEdit?.let { card ->
                        EditCardDialog(
                            card = card,
                            onDismiss = { cardToEdit = null },
                            onConfirm = { newFront, newBack ->
                                viewModel.onEvent(AllCardsEvent.EditCard(card, newFront, newBack))
                                cardToEdit = null
                            }
                        )
                    }

                    // Delete Card Dialog
                    cardToDelete?.let { card ->
                        AlertDialog(
                            onDismissRequest = { cardToDelete = null },
                            title = { Text("Delete Card") },
                            text = { Text("Are you sure you want to delete this card?") },
                            confirmButton = {
                                Button(onClick = {
                                    viewModel.onEvent(AllCardsEvent.DeleteCard(card))
                                    cardToDelete = null
                                }) { Text("Delete") }
                            },
                            dismissButton = {
                                TextButton(onClick = { cardToDelete = null }) { Text("Cancel") }
                            }
                        )
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
    onCardClick: (Long?) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick(card.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Front: ${card.front}")
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Back: ${card.back}")
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Card")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Card")
            }
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

@Composable
fun EditDeckDialog(
    deck: Deck?,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var deckName by remember { mutableStateOf(deck?.name ?: "") }
    var deckDescription by remember { mutableStateOf(deck?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Deck") },
        text = {
            Column {
                OutlinedTextField(
                    value = deckName,
                    onValueChange = { deckName = it },
                    label = { Text("Deck Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deckDescription,
                    onValueChange = { deckDescription = it },
                    label = { Text("Deck Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (deckName.isNotBlank()) {
                        onConfirm(deckName, deckDescription.ifBlank { null })
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    deckName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Deck") },
        text = { Text("Are you sure you want to delete the deck \"$deckName\"? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditCardDialog(
    card: Card,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var front by remember { mutableStateOf(card.front) }
    var back by remember { mutableStateOf(card.back) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Card") },
        text = {
            Column {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (front.isNotBlank() && back.isNotBlank()) {
                        onConfirm(front, back)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


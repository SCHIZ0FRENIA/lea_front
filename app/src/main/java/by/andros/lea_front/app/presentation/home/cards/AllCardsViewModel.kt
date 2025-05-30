package by.andros.lea_front.app.presentation.home.cards

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import by.andros.lea_front.app.data.Card
import by.andros.lea_front.app.data.CardRepository
import by.andros.lea_front.app.data.Deck
import by.andros.lea_front.app.data.DeckRepository
import by.andros.lea_front.app.data.PublicDecksRepository
import by.andros.lea_front.auth.domain.AuthRepository
import by.andros.lea_front.utils.getDaysSinceEpoch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AllCardsState {
    data object Loading : AllCardsState()
    data class Loaded(
        val cards: List<Card>,
        val deck: Deck? = null,
        val isUserLoggedIn: Boolean = false,
        val showDeleteConfirmation: Boolean = false,
        val showEditDeckDialog: Boolean = false,
        val showPublishConfirmation: Boolean = false,
        val navigationEvents: AllCardsNavigationEvent? = null, // For navigation after delete
        val message: String? = null
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
    data object PublishDeck : AllCardsEvent()
    data object ConfirmPublishDeck : AllCardsEvent()
    data object CancelPublishDeck : AllCardsEvent()
    data object ClearNavigationEvent : AllCardsEvent()
    data object ClearMessage : AllCardsEvent()
    data class EditCard(val card: Card, val newFront: String, val newBack: String) : AllCardsEvent()
    data class DeleteCard(val card: Card) : AllCardsEvent()
    data object ExportDeck : AllCardsEvent()
    data class ImportDeck(val uri: Uri) : AllCardsEvent()
}

sealed class AllCardsNavigationEvent {
    data object NavigateBack : AllCardsNavigationEvent()
}

@HiltViewModel
class AllCardsViewModel @Inject constructor(
    application: Application,
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    private val publicDecksRepository: PublicDecksRepository,
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

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
            is AllCardsEvent.PublishDeck -> showPublishConfirmation()
            is AllCardsEvent.ConfirmPublishDeck -> publishDeckToServer()
            is AllCardsEvent.CancelPublishDeck -> hidePublishConfirmation()
            is AllCardsEvent.ClearNavigationEvent -> clearNavigationEvent()
            is AllCardsEvent.ClearMessage -> clearMessage()
            is AllCardsEvent.EditCard -> editCard(event.card, event.newFront, event.newBack)
            is AllCardsEvent.DeleteCard -> deleteCard(event.card)
            is AllCardsEvent.ExportDeck -> {/* No-op, handled in Composable */}
            is AllCardsEvent.ImportDeck -> importDeck(event.uri)
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
                    AllCardsState.Loaded(
                        cards = cards, 
                        deck = deck, 
                        isUserLoggedIn = isLoggedIn
                    )
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

    private fun showPublishConfirmation() {
        (_state.value as? AllCardsState.Loaded)?.let {
            _state.value = it.copy(showPublishConfirmation = true)
        }
    }

    private fun hidePublishConfirmation() {
        (_state.value as? AllCardsState.Loaded)?.let {
            _state.value = it.copy(showPublishConfirmation = false)
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

    private fun clearMessage() {
        _state.update { currentState ->
            if (currentState is AllCardsState.Loaded) {
                currentState.copy(message = null)
            } else {
                currentState
            }
        }
    }

    private fun publishDeckToServer() {
        viewModelScope.launch {
            val currentState = _state.value as? AllCardsState.Loaded
            val currentDeck = currentState?.deck
            val cards = currentState?.cards
            
            if (currentDeck == null || cards.isNullOrEmpty()) {
                showMessage("No deck data to publish")
                return@launch
            }
            
            // Make sure user is logged in
            if (authRepository.getJwtToken() == null) {
                showMessage("You need to be logged in to publish decks")
                hidePublishConfirmation()
                return@launch
            }
            
            try {
                // Publish the deck to server
                val result = publicDecksRepository.publishDeck(currentDeck)
                
                result.fold(
                    onSuccess = { message ->
                        showMessage(message)
                    },
                    onFailure = { error ->
                        showMessage("Failed to publish deck: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                showMessage("Error publishing deck: ${e.message}")
            }
            
            hidePublishConfirmation()
        }
    }
    
    private fun showMessage(text: String) {
        _state.update { state ->
            if (state is AllCardsState.Loaded) {
                state.copy(message = text)
            } else {
                state
            }
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

    private fun importDeck(uri: Uri) {
        val context = getApplicationContext()
        viewModelScope.launch {
            try {
                val input = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@launch
                val lines = input.lines()
                var deckName = ""
                var deckDescription = ""
                val cards = mutableListOf<Pair<String, String>>()
                var currentCardFront = ""
                var currentCardBack = ""
                var mode = ""
                for (line in lines) {
                    when {
                        line.startsWith("# Deck name") -> mode = "deck_name"
                        line.startsWith("# Deck description") -> mode = "deck_description"
                        line.startsWith("## ") -> {
                            if (currentCardFront.isNotBlank() && currentCardBack.isNotBlank()) {
                                cards.add(currentCardFront to currentCardBack)
                                currentCardFront = ""
                                currentCardBack = ""
                            }
                            mode = "card_title"
                        }
                        line.startsWith("### Front") -> mode = "front"
                        line.startsWith("### Back") -> mode = "back"
                        else -> {
                            when (mode) {
                                "deck_name" -> { deckName = line; mode = "" }
                                "deck_description" -> { deckDescription = line; mode = "" }
                                "card_title" -> { currentCardFront = ""; currentCardBack = ""; mode = "" }
                                "front" -> { currentCardFront = line }
                                "back" -> { currentCardBack = line }
                            }
                        }
                    }
                }
                if (currentCardFront.isNotBlank() && currentCardBack.isNotBlank()) {
                    cards.add(currentCardFront to currentCardBack)
                }
                val newDeck = Deck(
                    name = deckName,
                    description = deckDescription,
                    createdBy = "imported"
                )
                val deckId = deckRepository.insertDeck(newDeck)
                for ((front, back) in cards) {
                    val card = Card(
                        front = front,
                        back = back,
                        lastReview = getDaysSinceEpoch().toInt(),
                        nextReview = getDaysSinceEpoch().toInt(),
                        deckId = deckId
                    )
                    cardRepository.insertCard(card)
                }
                Toast.makeText(context, "Deck imported!", Toast.LENGTH_SHORT).show()
                loadAllData()
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getApplicationContext(): Context = getApplication()
} 
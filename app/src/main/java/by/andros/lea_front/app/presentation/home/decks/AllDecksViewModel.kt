package by.andros.lea_front.app.presentation.home.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.andros.lea_front.app.data.Deck
import by.andros.lea_front.app.data.DeckRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class AllDecksState {
    data object Loading : AllDecksState()
    data class Loaded(val decks: List<Deck>) : AllDecksState()
    data class Error(val message: String) : AllDecksState()
}

sealed class AllDecksEvent {
    data object LoadDecks : AllDecksEvent()
    data class DeckSelected(val deckId: Long) : AllDecksEvent()
    data class CreateDeck(val name: String, val description: String?) : AllDecksEvent()
}

@HiltViewModel
class AllDecksViewModel @Inject constructor(
    private val deckRepository: DeckRepository
) : ViewModel() {
    private val _state = MutableStateFlow<AllDecksState>(AllDecksState.Loading)
    val state: StateFlow<AllDecksState> = _state

    init {
        onEvent(AllDecksEvent.LoadDecks)
    }

    fun onEvent(event: AllDecksEvent) {
        when (event) {
            is AllDecksEvent.LoadDecks -> loadDecks()
            is AllDecksEvent.DeckSelected -> onDeckSelected(event.deckId)
            is AllDecksEvent.CreateDeck -> createDeck(event.name, event.description)
        }
    }

    private fun loadDecks() {
        viewModelScope.launch {
            _state.value = AllDecksState.Loading
            deckRepository.getAllDecks()
                .catch { e ->
                    _state.value = AllDecksState.Error("Error loading decks: ${e.message}")
                }
                .collect { decks ->
                    _state.value = AllDecksState.Loaded(decks)
                }
        }
    }

    private fun onDeckSelected(deckId: Long) {
    }

    private fun createDeck(name: String, description: String?) {
        viewModelScope.launch {
            val newDeck = Deck(
                name = name,
                description = description,
                createdBy = "gibberish_user",
                source = "user"
            )
            deckRepository.insertDeck(newDeck)
            loadDecks()
        }
    }
}

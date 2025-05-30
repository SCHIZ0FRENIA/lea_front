package by.andros.lea_front.app.presentation.home.decks

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.andros.lea_front.app.data.Card
import by.andros.lea_front.app.data.Deck
import by.andros.lea_front.app.data.DeckRepository
import by.andros.lea_front.app.data.CardRepository
import by.andros.lea_front.utils.getDaysSinceEpoch
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
    private val deckRepository: DeckRepository,
    private val cardRepository: by.andros.lea_front.app.data.CardRepository
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

    fun importDeckFromUri(context: Context, uri: Uri) {
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
                    val trimmedLine = line.trim()
                    when {
                        trimmedLine.startsWith("# Deck name") -> mode = "deck_name"
                        trimmedLine.startsWith("# Deck description") -> mode = "deck_description"
                        trimmedLine.startsWith("## ") -> {
                            if (currentCardFront.isNotBlank() || currentCardBack.isNotBlank()) {
                                cards.add(currentCardFront to currentCardBack)
                                currentCardFront = ""
                                currentCardBack = ""
                            }
                            mode = "card_title"
                        }
                        trimmedLine.startsWith("### Front") -> mode = "front"
                        trimmedLine.startsWith("### Back") -> mode = "back"
                        trimmedLine.isBlank() -> { /* skip blank lines */ }
                        else -> {
                            when (mode) {
                                "deck_name" -> { deckName = trimmedLine; mode = "" }
                                "deck_description" -> { deckDescription = trimmedLine; mode = "" }
                                "card_title" -> { /* skip, handled by ## */ }
                                "front" -> { currentCardFront = trimmedLine }
                                "back" -> { currentCardBack = trimmedLine }
                            }
                        }
                    }
                }
                if (currentCardFront.isNotBlank() || currentCardBack.isNotBlank()) {
                    cards.add(currentCardFront to currentCardBack)
                }
                val newDeck = by.andros.lea_front.app.data.Deck(
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
                loadDecks()
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

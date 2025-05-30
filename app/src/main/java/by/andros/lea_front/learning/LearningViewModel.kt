package by.andros.lea_front.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.andros.lea_front.app.data.Card
import by.andros.lea_front.app.data.DeckRepository
import by.andros.lea_front.app.data.CardRepository
import by.andros.lea_front.app.data.StatisticsRepository
import by.andros.lea_front.app.data.model.DeckWithCardCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI States ---
sealed class LearningUiState {
    object Idle : LearningUiState()
    data class DeckSelection(val allDecksWithCount: List<DeckWithCardCount>, val selectedDeckIds: Set<Long>) : LearningUiState()
    data class InProgress(val currentCard: Card, val showAnswer: Boolean, val progressText: String) : LearningUiState()
    data class Paused(val pausedState: InternalLearningSessionState) : LearningUiState()
    data class SessionFinished(val stats: LearningSessionStats) : LearningUiState()
    data class Error(val message: String) : LearningUiState()
}

// --- User Events ---
sealed class LearningUserEvent {
    data class ToggleDeckSelection(val deckId: Long) : LearningUserEvent()
    object StartLearningSession : LearningUserEvent()
    object ShowCardAnswer : LearningUserEvent()
    data class SubmitCardAnswer(val rating: Int) : LearningUserEvent()
    object PauseSession : LearningUserEvent()
    object ResumeSession : LearningUserEvent()
    object EndSessionAndExit : LearningUserEvent()
    object ReturnToDeckSelection : LearningUserEvent()
    object RefreshDeckData : LearningUserEvent()
}

@HiltViewModel
class LearningViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    private val cardRepository: CardRepository,
    private val statisticsRepository: StatisticsRepository,
    private val learningService: LearningService
) : ViewModel() {

    private val _uiState = MutableStateFlow<LearningUiState>(LearningUiState.Idle)
    val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()

    private var currentSelectedDeckIds = mutableSetOf<Long>()
    private val refreshDecksTrigger = MutableStateFlow(true) // Initial value to trigger on init

    init {
        setupDeckObservation()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupDeckObservation() {
        viewModelScope.launch {
            refreshDecksTrigger
                .flatMapLatest {
                    deckRepository.getAllDecks()
                        .catch { e ->
                            _uiState.value = LearningUiState.Error("Failed to load decks stream: ${e.message}")
                            emit(emptyList<by.andros.lea_front.app.data.Deck>())
                        }
                }
                .catch { e ->
                    _uiState.value = LearningUiState.Error("Major error in deck observation setup: ${e.message}")
                }
                .collect { decks ->
                    if (_uiState.value is LearningUiState.Error && decks.isEmpty()) {
                        return@collect
                    }
                    try {
                        val decksWithCounts = decks.mapNotNull { deck ->
                            deck.id?.let { deckId ->
                                try {
                                    val count = deckRepository.getCardCountForDeck(deckId).first()
                                    DeckWithCardCount(deck = deck, cardCount = count)
                                } catch (e: Exception) {
                                    DeckWithCardCount(deck = deck, cardCount = -1)
                                }
                            }
                        }

                        val validSelectedIds = decksWithCounts.mapNotNull { it.deck.id }.toSet()
                        currentSelectedDeckIds.retainAll(validSelectedIds)

                        val deckSelectionState = LearningUiState.DeckSelection(decksWithCounts, currentSelectedDeckIds.toSet())
                        _uiState.value = deckSelectionState
                    } catch (e: Exception) {
                        _uiState.value = LearningUiState.Error("Failed to process deck data: ${e.message}")
                    }
                }
        }
    }

    fun onEvent(event: LearningUserEvent) {
        when (event) {
            is LearningUserEvent.ToggleDeckSelection -> handleToggleDeckSelection(event.deckId)
            LearningUserEvent.StartLearningSession -> startNewSession()
            LearningUserEvent.ShowCardAnswer -> showAnswerInternal()
            is LearningUserEvent.SubmitCardAnswer -> submitAnswerInternal(event.rating)
            LearningUserEvent.PauseSession -> pauseSessionInternal()
            LearningUserEvent.ResumeSession -> resumeSessionInternal()
            LearningUserEvent.EndSessionAndExit -> endSessionAndGoToDeckSelection()
            LearningUserEvent.ReturnToDeckSelection -> goToDeckSelection()
            LearningUserEvent.RefreshDeckData -> triggerDeckRefresh()
        }
    }

    private fun handleToggleDeckSelection(deckId: Long) {
        val currentDeckSelectionState = _uiState.value as? LearningUiState.DeckSelection
        if (currentDeckSelectionState == null) {
            return
        }

        val mutableSelectedIds = currentSelectedDeckIds.toMutableSet()
        if (mutableSelectedIds.contains(deckId)) {
            mutableSelectedIds.remove(deckId)
        } else {
            mutableSelectedIds.add(deckId)
        }
        currentSelectedDeckIds = mutableSelectedIds
        val newState = LearningUiState.DeckSelection(currentDeckSelectionState.allDecksWithCount, currentSelectedDeckIds.toSet())
        _uiState.value = newState
    }

    private fun triggerDeckRefresh() {
        refreshDecksTrigger.value = !refreshDecksTrigger.value // Toggle the value
    }

    private fun startNewSession() {
        val currentDeckSelectionState = _uiState.value as? LearningUiState.DeckSelection
        if (currentSelectedDeckIds.isEmpty()) {
            _uiState.value = LearningUiState.Error("Please select at least one deck.")
            if (currentDeckSelectionState == null) {
                // Intentionally blank
            } else {
                 _uiState.value = LearningUiState.DeckSelection(currentDeckSelectionState.allDecksWithCount, currentSelectedDeckIds.toSet())
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = LearningUiState.Idle
            val cardsForSession = mutableListOf<Card>()
            try {
                currentSelectedDeckIds.forEach { deckId ->
                    val cards = cardRepository.getCardsByDeck(deckId).first()
                    cardsForSession.addAll(cards)
                }
                if (cardsForSession.isEmpty()) {
                    _uiState.value = LearningUiState.Error("No cards found in the selected deck(s).")
                    triggerDeckRefresh()
                    return@launch
                }
                
                // Start recording session in statistics
                statisticsRepository.startLearningSession(cardsForSession.size)
                
                learningService.startSession(cardsForSession)
                updateUiForCurrentCard()
            } catch (e: Exception) {
                _uiState.value = LearningUiState.Error("Error starting session: ${e.message}")
                triggerDeckRefresh()
            }
        }
    }

    private fun showAnswerInternal() {
        learningService.showAnswer()
        updateUiForCurrentCard(showAnswer = true)
    }

    private fun submitAnswerInternal(rating: Int) {
        val currentCard = learningService.getCurrentCard()
        
        // Record the answer for statistics
        viewModelScope.launch {
            currentCard?.id?.let { cardId ->
                statisticsRepository.recordCardAnswer(cardId, rating)
            }
        }
        
        learningService.submitAnswer(rating)
        if (learningService.isSessionFinished()) {
            val stats = learningService.getStats()
            
            // End the session in statistics
            viewModelScope.launch {
                statisticsRepository.endLearningSession()
            }
            
            if (stats != null) {
                _uiState.value = LearningUiState.SessionFinished(stats)
            } else {
                _uiState.value = LearningUiState.Error("Error retrieving session stats.")
            }
        } else {
            updateUiForCurrentCard()
        }
    }

    private fun updateUiForCurrentCard(showAnswer: Boolean = false) {
        val card = learningService.getCurrentCard()
        val progress = learningService.getCurrentProgress()
        if (card != null && progress != null) {
            _uiState.value = LearningUiState.InProgress(card, showAnswer, "${progress.first} / ${progress.second}")
        } else {
            if (learningService.isSessionFinished()) {
                 val stats = learningService.getStats()
                 if (stats != null) _uiState.value = LearningUiState.SessionFinished(stats)
                 else endSessionAndGoToDeckSelection()
            } else {
                 _uiState.value = LearningUiState.Error("Error displaying card.")
                 refreshDecksTrigger.value = !refreshDecksTrigger.value // Ensure toggle here as well
            }
        }
    }

    private fun pauseSessionInternal() {
        val pausedState = learningService.pauseSession()
        if (pausedState != null) {
            _uiState.value = LearningUiState.Paused(pausedState)
        } else {
            _uiState.value = LearningUiState.Error("No active session to pause.")
        }
    }

    private fun resumeSessionInternal() {
        val currentStateValue = _uiState.value
        if (currentStateValue is LearningUiState.Paused) {
            if (currentSelectedDeckIds.isNotEmpty()) {
                viewModelScope.launch {
                    _uiState.value = LearningUiState.Idle
                    val freshCardsForSession = mutableListOf<Card>()
                    try {
                        currentSelectedDeckIds.forEach { deckId ->
                            val cards = cardRepository.getCardsByDeck(deckId).first()
                            freshCardsForSession.addAll(cards)
                        }
                        if (freshCardsForSession.isEmpty()) {
                            _uiState.value = LearningUiState.Error("No cards found for resumed session.")
                            triggerDeckRefresh()
                            return@launch
                        }
                        learningService.startSession(freshCardsForSession)
                        updateUiForCurrentCard()
                    } catch (e: Exception) {
                        _uiState.value = LearningUiState.Error("Error resuming session: ${e.message}")
                        triggerDeckRefresh()
                    }
                }
            } else {
                 _uiState.value = LearningUiState.Error("Cannot resume: No decks identified.")
                 triggerDeckRefresh()
            }
        } else {
            _uiState.value = LearningUiState.Error("No session to resume.")
        }
    }

    private fun endSessionAndGoToDeckSelection() {
        // End the session in statistics
        viewModelScope.launch {
            statisticsRepository.endLearningSession()
        }
        
        learningService.endSession()
        currentSelectedDeckIds.clear()
        _uiState.value = LearningUiState.Idle
        triggerDeckRefresh()
    }

    private fun goToDeckSelection(){
        currentSelectedDeckIds.clear()
        _uiState.value = LearningUiState.Idle
        triggerDeckRefresh()
    }
} 
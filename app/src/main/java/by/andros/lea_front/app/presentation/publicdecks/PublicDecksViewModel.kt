package by.andros.lea_front.app.presentation.publicdecks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.andros.lea_front.app.data.PublicDecksRepository
import by.andros.lea_front.app.data.model.PublicDeckResponse
import by.andros.lea_front.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class PublicDecksState(
    val isLoading: Boolean = false,
    val decks: List<PublicDeckResponse> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val selectedDeck: PublicDeckResponse? = null,
    val message: String? = null,
    val isAdmin: Boolean = false,
    val sessionExpired: Boolean = false
)

sealed class PublicDecksEvent {
    data object LoadDecks : PublicDecksEvent()
    data class SearchDecks(val query: String) : PublicDecksEvent()
    data class SelectDeck(val deck: PublicDeckResponse) : PublicDecksEvent()
    data class DeleteDeck(val deckId: String) : PublicDecksEvent()
    data class DownloadDeck(val deckId: String) : PublicDecksEvent()
    data class PublishDeck(val deckId: Long) : PublicDecksEvent()
    data object ClearMessage : PublicDecksEvent()
    
    sealed class Navigation {
        data object GoToHome : Navigation()
    }
}

@HiltViewModel
class PublicDecksViewModel @Inject constructor(
    private val publicDecksRepository: PublicDecksRepository,
    private val authRepository: AuthRepository,
    private val deckRepository: by.andros.lea_front.app.data.DeckRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(PublicDecksState())
    val state: StateFlow<PublicDecksState> = _state.asStateFlow()
    
    private val _navigationEvents = MutableSharedFlow<PublicDecksEvent.Navigation>()
    val navigationEvents = _navigationEvents.asSharedFlow()
    
    init {
        // Check if user is admin
        _state.update { it.copy(isAdmin = authRepository.isUserAdmin()) }
        
        // Load decks on initialization
        onEvent(PublicDecksEvent.LoadDecks)
    }
    
    // Explicitly set admin mode (used when navigating from admin panel)
    fun setAdminMode(isAdmin: Boolean) {
        _state.update { it.copy(isAdmin = isAdmin) }
    }
    
    fun onEvent(event: PublicDecksEvent) {
        when (event) {
            is PublicDecksEvent.LoadDecks -> loadDecks()
            is PublicDecksEvent.SearchDecks -> searchDecks(event.query)
            is PublicDecksEvent.SelectDeck -> selectDeck(event.deck)
            is PublicDecksEvent.DeleteDeck -> deleteDeck(event.deckId)
            is PublicDecksEvent.DownloadDeck -> downloadDeck(event.deckId)
            is PublicDecksEvent.PublishDeck -> publishDeck(event.deckId)
            is PublicDecksEvent.ClearMessage -> clearMessage()
        }
    }
    
    private fun updateErrorState(error: Throwable) {
        val errorMessage = error.message ?: "An unknown error occurred"
        val isSessionExpired = errorMessage.contains("session has expired", ignoreCase = true) ||
                             errorMessage.contains("log in again", ignoreCase = true)
        
        _state.update { it.copy(
            isLoading = false,
            isSearching = false,
            error = errorMessage,
            sessionExpired = isSessionExpired
        )}
    }
    
    private fun loadDecks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, sessionExpired = false) }
            
            publicDecksRepository.getAllPublicDecks().collect { result ->
                result.fold(
                    onSuccess = { decks ->
                        _state.update { it.copy(
                            isLoading = false,
                            decks = decks,
                            error = null
                        )}
                    },
                    onFailure = { error ->
                        updateErrorState(error)
                    }
                )
            }
        }
    }
    
    private fun searchDecks(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(
                isSearching = true, 
                searchQuery = query,
                error = null,
                sessionExpired = false
            )}
            
            if (query.isBlank()) {
                loadDecks()
                return@launch
            }
            
            publicDecksRepository.searchPublicDecks(query).collect { result ->
                result.fold(
                    onSuccess = { decks ->
                        _state.update { it.copy(
                            isSearching = false,
                            decks = decks,
                            error = null
                        )}
                    },
                    onFailure = { error ->
                        updateErrorState(error)
                    }
                )
            }
        }
    }
    
    private fun selectDeck(deck: PublicDeckResponse) {
        _state.update { it.copy(selectedDeck = deck) }
    }
    
    private fun deleteDeck(deckId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, sessionExpired = false) }
            
            val result = publicDecksRepository.deletePublicDeck(deckId)
            
            result.fold(
                onSuccess = { message ->
                    _state.update { it.copy(
                        isLoading = false,
                        message = message,
                        selectedDeck = null
                    )}
                    loadDecks()
                },
                onFailure = { error ->
                    updateErrorState(error)
                }
            )
        }
    }
    
    private fun downloadDeck(deckId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, sessionExpired = false) }
            
            val result = publicDecksRepository.downloadPublicDeck(deckId)
            
            result.fold(
                onSuccess = { message ->
                    _state.update { it.copy(
                        isLoading = false,
                        message = message
                    )}
                    // Trigger a refresh of the deck list in the home page
                    deckRepository.triggerRefresh()
                    // Emit navigation event to go back to home and refresh the deck list
                    _navigationEvents.emit(PublicDecksEvent.Navigation.GoToHome)
                },
                onFailure = { error ->
                    updateErrorState(error)
                }
            )
        }
    }
    
    private fun publishDeck(deckId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, sessionExpired = false) }
            
            try {
                // First get the deck from local database
                val deck = deckRepository.getDeckById(deckId).first()
                
                // Publish the deck
                val result = publicDecksRepository.publishDeck(deck)
                
                result.fold(
                    onSuccess = { message ->
                        _state.update { it.copy(
                            isLoading = false,
                            message = message
                        )}
                    },
                    onFailure = { error ->
                        updateErrorState(error)
                    }
                )
            } catch (e: Exception) {
                updateErrorState(e)
            }
        }
    }
    
    private fun clearMessage() {
        _state.update { it.copy(message = null, error = null, sessionExpired = false) }
    }
} 
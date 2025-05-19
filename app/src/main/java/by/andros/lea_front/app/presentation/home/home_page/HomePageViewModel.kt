package by.andros.lea_front.app.presentation.home.home_page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.andros.lea_front.app.data.Deck
import by.andros.lea_front.app.data.DeckRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class HomePageState {
    data object Loading : HomePageState()
    data class Loaded(val decks: List<Deck>) : HomePageState()
    data class Error(val message: String) : HomePageState()
}

sealed class HomePageEvent {
    data object LoadDecks : HomePageEvent()
    data class DeckSelected(val deckId: Long) : HomePageEvent()
    data object ShowAllDecks : HomePageEvent()
    data object DeckDetails : HomePageEvent()

    sealed class Navigation {
        data object ToAllDecks : Navigation()
        data class ToDeckDetails(val deckId: Long) : Navigation()
        data class ToCards(val deckId: Long) : Navigation()
    }
}

@HiltViewModel
class HomePageViewModel @Inject constructor(
    private val deckRepository: DeckRepository
) : ViewModel() {
    private val _state = MutableStateFlow<HomePageState>(HomePageState.Loading)
    val state: StateFlow<HomePageState> = _state

    private val _events = MutableSharedFlow<HomePageEvent.Navigation>()
    val events = _events.asSharedFlow()

    init {
        onEvent(HomePageEvent.LoadDecks)
    }

    fun onEvent(event: HomePageEvent) {
        when (event) {
            is HomePageEvent.LoadDecks -> loadDecks()
            is HomePageEvent.DeckSelected -> navigateToCards(event.deckId)
            is HomePageEvent.ShowAllDecks -> navigateToShowAllDecks()
            is HomePageEvent.DeckDetails -> {  }
        }
    }

    private fun loadDecks() {
        viewModelScope.launch {
            _state.value = HomePageState.Loading
            deckRepository.getAllDecks()
                .catch { e ->
                    _state.value = HomePageState.Error("Error: ${e.message}")
                }
                .collect { decks ->
                    _state.value = HomePageState.Loaded(decks)
                }
        }
    }

    private fun navigateToShowAllDecks() {
        viewModelScope.launch {
            _events.emit(HomePageEvent.Navigation.ToAllDecks)
        }
    }

    private fun navigateToCards(deckId: Long) {
        viewModelScope.launch {
            _events.emit(HomePageEvent.Navigation.ToCards(deckId))
        }
    }
}

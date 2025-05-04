package by.andros.lea_front.app.presentation.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update


sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Learning : Screen("Learning")
}

data class HomeScreenState(
    val currentRoute: String = Screen.Home.route,
    val isLoading: Boolean = false,
)

sealed class HomeScreenEvent {
    data object NavigateToHome : HomeScreenEvent()
    data object NavigateToLearning : HomeScreenEvent()
    data class SyncRoute(val route: String) : HomeScreenEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(

) : ViewModel() {
    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state
    
    fun onEvent(event: HomeScreenEvent) {
        when (event) {
            is HomeScreenEvent.NavigateToHome -> updateRoute(Screen.Home.route)
            is HomeScreenEvent.NavigateToLearning -> updateRoute(Screen.Learning.route)
            is HomeScreenEvent.SyncRoute -> updateRoute(event.route)
        }
    }
    
    private fun updateRoute(route: String) {
        _state.update { it.copy(currentRoute = route) }
    }
}
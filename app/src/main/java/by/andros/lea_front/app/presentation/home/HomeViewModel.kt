package by.andros.lea_front.app.presentation.home

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.andros.lea_front.auth.domain.AuthRepository // Import AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Learning : Screen("Learning")
}

data class HomeScreenState(
    val currentRoute: String = Screen.Home.route,
    val isLoading: Boolean = false,
    val currentUser: String,
)

sealed class HomeScreenEvent {
    data object NavigateToHome : HomeScreenEvent()
    data object NavigateToLearning : HomeScreenEvent()
    data class SyncRoute(val route: String) : HomeScreenEvent()
    data object Logout : HomeScreenEvent()

    sealed class Navigation {
        data object ToHome: Navigation()
        data object ToLearning: Navigation()
        data object ToLogin: Navigation()
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(HomeScreenState(currentUser = sharedPreferences.getString(
        "login",
        "default"
    )!!))
    val state: StateFlow<HomeScreenState> = _state

    private val _events = MutableSharedFlow<HomeScreenEvent.Navigation>()
    val events = _events.asSharedFlow()

    fun onEvent(event: HomeScreenEvent) {
        when (event) {
            is HomeScreenEvent.NavigateToHome -> updateRoute(Screen.Home.route)
            is HomeScreenEvent.NavigateToLearning -> updateRoute(Screen.Learning.route)
            is HomeScreenEvent.SyncRoute -> updateRoute(event.route)
            is HomeScreenEvent.Logout -> logout()
        }
    }

    private fun updateRoute(route: String) {
        _state.update { it.copy(currentRoute = route) }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.clearAuthData()
            _events.emit(HomeScreenEvent.Navigation.ToLogin)
        }
    }
}

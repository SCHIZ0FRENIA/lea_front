package by.andros.lea_front.auth.presentation.login

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.andros.lea_front.auth.domain.AuthRepository
import by.andros.lea_front.auth.domain.LoginUseCase
import by.andros.lea_front.auth.domain.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val authRepository: AuthRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    private val _events = MutableSharedFlow<LoginEvent.Navigation>()
    val events = _events.asSharedFlow()

    private val _launchGoogleSignInEvents = MutableSharedFlow<Unit>() // New SharedFlow for launching browser
    val launchGoogleSignInEvents = _launchGoogleSignInEvents.asSharedFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.LoginChanged -> _state.update { it.copy(login = event.login, error = null) }
            is LoginEvent.PasswordChanged -> _state.update { it.copy(password = event.password, error = null) }
            is LoginEvent.Submit -> login()
            is LoginEvent.ToRegistration -> {}
            is LoginEvent.WithoutAuth -> navigateToHomeWithoutAuth()
            is LoginEvent.GoogleSignIn -> TODO()
        }
    }

    private fun login() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, isSuccess = false) }
            val result = loginUseCase(state.value.login, state.value.password)
            result.onSuccess {
                _state.update { it.copy(isLoading = false, isSuccess = true) }
                _events.emit(LoginEvent.Navigation.ToHome)
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = if (e.message.isNullOrBlank()) "Login failed. Please check your credentials." else e.message) }
            }
        }
    }

    private fun navigateToHomeWithoutAuth() {
        viewModelScope.launch {
            authRepository.clearAuthData()
            _events.emit(LoginEvent.Navigation.ToHome)
        }
    }


}

package by.andros.lea_front.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.andros.lea_front.auth.domain.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state
    
    private val _events = MutableSharedFlow<LoginEvent.Navigation>()
    val events = _events.asSharedFlow()
    
    fun onEvent(event: LoginEvent) {
        when(event){
            is LoginEvent.LoginChanged -> updateLogin(event.login)
            is LoginEvent.PasswordChanged -> updatePassword(event.password)
            is LoginEvent.Submit -> login()
            is LoginEvent.ToRegistration -> _events.tryEmit(LoginEvent.Navigation.ToRegistration)
            is LoginEvent.WithoutAuth -> _events.tryEmit(LoginEvent.Navigation.ToHome)
            is LoginEvent.GoogleSignIn -> googleSignIn()
        }
    }
    
    private fun updateLogin(login: String) {
        _state.update { currentState ->
            currentState.copy(
                login = login,
                error = if (login.isBlank()) "Login cannot be empty" else null
            )
        }
    }
    
    private fun updatePassword(password: String) {
        _state.update { currentState ->
            currentState.copy(
                password = password,
                error = when {
                    password.isBlank() -> "Password cannot be empty"
                    password.length < 8 -> "Password too short"
                    else -> null
                }
            )
        }
    }
    
    private fun login() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            loginUseCase(
                _state.value.login,
                _state.value.password
            ).fold(
                onSuccess = {
                    _state.update { state ->
                        state.copy(
                            isSuccess = true,
                            isLoading = false,
                            login = "",
                            password = ""
                        )
                    }
                    _events.emit(LoginEvent.Navigation.ToHome)
                },
                onFailure = { error ->
                    _state.update { state ->
                        state.copy(
                            error = error.message ?: "Login failed",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }
    
    private fun googleSignIn() {
        //TODO authenticate with fucking google
    }
}
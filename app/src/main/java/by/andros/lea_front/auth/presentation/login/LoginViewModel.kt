package by.andros.lea_front.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel: ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state
    
    fun onEvent(event: LoginEvent) {
        when(event){
            is LoginEvent.LoginChanged -> updateLogin(event.login)
            is LoginEvent.PasswordChanged -> updatePassword(event.password)
            is LoginEvent.Submit -> login()
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
        val currentLogin = _state.value.login
        val currentPassword = _state.value.password
        
        if (currentLogin.isBlank() || currentPassword.isBlank() || currentPassword.length < 8) {
            _state.update {
                it.copy(
                    error = when {
                        currentPassword.isBlank() -> "Password cannot be empty"
                        currentPassword.length < 8 -> "Password too short"
                        else -> null
                    }
                )
            }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                
                //TODO MAKE LOGIN REQUEST
                
                _state.update { it.copy(isSuccess = true, isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Login failed"
                    )
                }
            }
        }
    }
}
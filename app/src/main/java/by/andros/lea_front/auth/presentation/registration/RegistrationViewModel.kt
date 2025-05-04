package by.andros.lea_front.auth.presentation.registration

import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.andros.lea_front.auth.domain.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel()  {
    private val _state = MutableStateFlow(RegistrationState())
    val state: StateFlow<RegistrationState> = _state
    
    private val _events = MutableSharedFlow<RegistrationEvent.Navigation>()
    val events = _events.asSharedFlow()
    
    
    fun onEvent(event: RegistrationEvent) {
        when(event){
            is RegistrationEvent.LoginChanged -> updateLogin(event.login)
            is RegistrationEvent.PasswordChanged -> updatePassword(event.password)
            is RegistrationEvent.ConfirmationChanged -> updateConfirmation(event.confirmation)
            
            is RegistrationEvent.Register -> register()
        }
    }
    
    private fun register() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null)}
            
            registerUseCase(
                login = _state.value.login,
                password = _state.value.password,
            ).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isSuccess = true,
                            isLoading = false,
                            login = "",
                            password = "",
                            confirmation = ""
                        )
                    }
                    _events.emit(RegistrationEvent.Navigation.ToHome)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            error = error.message ?: "Registration failed",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }
    
    private fun updateLogin(login: String) {
        _state.update { currentState ->
            currentState.copy(
                login = login,
                error = if (login.isBlank()) "Login can't be empty" else null
            )
        }
    }
    
    private fun updatePassword(password: String) {
        _state.update { currentState ->
            currentState.copy(
                password = password,
                error = if (password.isBlank()) "Password can't be empty" else null
            )
        }
    }
    
    private fun updateConfirmation(confirmation: String) {
        _state.update { currentState ->
            currentState.copy(
                confirmation = confirmation,
                error =
                    when {
                        confirmation.isBlank() -> "Confirmation can't be blank"
                        confirmation != currentState.password -> "Passwords doesn't match"
                        else -> null
                    }
            )
        }
    }
}
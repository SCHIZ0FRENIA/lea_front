package by.andros.lea_front.auth.presentation.login

sealed class LoginEvent {
    data class LoginChanged(val login: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    data object Submit : LoginEvent()
}

package by.andros.lea_front.auth.presentation.login

sealed class LoginEvent {
    data class LoginChanged(val login: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    data object Submit : LoginEvent()
    data class ToRegistration(
        val login: String,
        val password: String
    ) : LoginEvent()
    data object WithoutAuth: LoginEvent()
    data object GoogleSignIn: LoginEvent()
    
    sealed class Navigation {
        data object ToHome: Navigation()
        data object ToRegistration: Navigation()
    }
}

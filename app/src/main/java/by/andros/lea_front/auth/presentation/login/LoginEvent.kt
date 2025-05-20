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
    // Modified GoogleSignIn event to optionally carry the authCode
    data class GoogleSignIn(val authCode: String? = null) : LoginEvent()

    sealed class Navigation {
        data object ToHome: Navigation()
        data object ToRegistration: Navigation()
        data object LaunchGoogleSignIn: Navigation() // This was the missing reference in LoginScreen
    }
}

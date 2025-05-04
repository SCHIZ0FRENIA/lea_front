package by.andros.lea_front.auth.presentation.registration

sealed class RegistrationEvent {
    data class LoginChanged(val login : String) : RegistrationEvent()
    data class PasswordChanged(val password : String) : RegistrationEvent()
    data class ConfirmationChanged(val confirmation : String) : RegistrationEvent()
    
    data object Register : RegistrationEvent()

    sealed class Navigation {
        data object ToHome : Navigation()
        data object ToSignIn : Navigation()
    }
}
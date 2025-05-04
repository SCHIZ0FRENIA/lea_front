package by.andros.lea_front.auth.presentation.registration

data class RegistrationState (
    val login: String = "",
    val password: String = "",
    val confirmation: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
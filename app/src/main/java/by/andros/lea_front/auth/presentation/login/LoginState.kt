package by.andros.lea_front.auth.presentation.login

data class LoginState (
    val login: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
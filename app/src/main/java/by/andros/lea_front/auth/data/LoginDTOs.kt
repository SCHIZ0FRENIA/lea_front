package by.andros.lea_front.auth.data

data class LoginRequest (
    val name: String,
    val password: String
)

data class LoginResponse(
    val login: String,
    val role: String,
    val token: String,
)
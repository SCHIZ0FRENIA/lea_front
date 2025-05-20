package by.andros.lea_front.auth.data

data class LoginRequest (
    val login: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val token: String,
    val role: String,
)

data class GoogleLoginRequest(
    val auth_code: String,
)
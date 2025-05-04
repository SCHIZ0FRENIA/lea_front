package by.andros.lea_front.auth.data

data class RegisterRequest(
    val login: String,
    val password: String,
    val role: String,
)

data class RegisterResponse(
    val login: String,
    val role: String,
    val token: String,
)
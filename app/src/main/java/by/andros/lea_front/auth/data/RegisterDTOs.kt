package by.andros.lea_front.auth.data

data class RegisterRequest(
    val login: String,
    val password: String,
    val role: String,
)

data class RegisterResponse(
    val message: String,
    val token: String,
    val role: String,
)
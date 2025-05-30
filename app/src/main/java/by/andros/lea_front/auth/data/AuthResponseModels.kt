package by.andros.lea_front.auth.data

data class LoginRequest(
    val login: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val token: String,
    val role: String
)

data class RegisterRequest(
    val login: String,
    val password: String,
    val role: String
)

data class RegisterResponse(
    val message: String,
    val token: String,
    val role: String
)

data class GoogleLoginRequest(
    val idToken: String
)

data class UserDto(
    val userId: String,
    val login: String,
    val role: String
)

data class UserListResponse(
    val users: List<UserDto>
)

data class UserRoleResponse(
    val userId: String,
    val login: String,
    val role: String,
    val message: String
)

data class DeleteUserResponse(
    val userId: String,
    val message: String
) 
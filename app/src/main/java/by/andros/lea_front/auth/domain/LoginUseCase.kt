package by.andros.lea_front.auth.domain

import by.andros.lea_front.auth.data.LoginResponse
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(login: String, password: String): Result<LoginResponse> {
        if (login.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be blank"))
        }
        return repository.login(login, password)
    }
}
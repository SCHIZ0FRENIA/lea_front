package by.andros.lea_front.auth.domain

import by.andros.lea_front.auth.data.RegisterResponse
import jakarta.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(login: String, password: String): Result<RegisterResponse> {
        if (login.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be blank"))
        }
        return repository.register(login, password)
    }
}
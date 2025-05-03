package by.andros.lea_front.auth.domain

import by.andros.lea_front.auth.domain.AuthRepository

class LoginUseCase (private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        return repository.login(email, password)
    }
}
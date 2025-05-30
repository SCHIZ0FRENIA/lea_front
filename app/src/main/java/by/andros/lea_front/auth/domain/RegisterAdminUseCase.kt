package by.andros.lea_front.auth.domain

import by.andros.lea_front.auth.data.RegisterResponse
import javax.inject.Inject

class RegisterAdminUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(login: String, password: String): Result<RegisterResponse> {
        if (login.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Login and password cannot be blank"))
        }
        
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters long"))
        }
        
        // Check if the current user is admin
        if (!repository.isUserAdmin()) {
            return Result.failure(SecurityException("Only administrators can create admin accounts"))
        }
        
        return repository.registerAdmin(login, password)
    }
} 
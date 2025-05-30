package by.andros.lea_front.auth.domain

import by.andros.lea_front.auth.data.UserRoleResponse
import javax.inject.Inject

class GrantAdminRoleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(userId: String): Result<UserRoleResponse> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        
        // Check if the current user is admin
        if (!repository.isUserAdmin()) {
            return Result.failure(SecurityException("Only administrators can grant admin privileges"))
        }
        
        return repository.grantAdminRole(userId)
    }
} 
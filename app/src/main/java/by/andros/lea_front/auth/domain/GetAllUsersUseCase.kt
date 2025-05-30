package by.andros.lea_front.auth.domain

import by.andros.lea_front.auth.data.UserDto
import javax.inject.Inject

class GetAllUsersUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<List<UserDto>> {
        return authRepository.getAllUsers()
    }
} 
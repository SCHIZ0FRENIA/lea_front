package by.andros.lea_front.auth.domain

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
}
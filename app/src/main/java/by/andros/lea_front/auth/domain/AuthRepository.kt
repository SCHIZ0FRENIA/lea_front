package by.andros.lea_front.auth.domain

import by.andros.lea_front.auth.data.LoginResponse
import by.andros.lea_front.auth.data.RegisterResponse

interface AuthRepository {
    suspend fun login(login: String, password: String): Result<LoginResponse>
    suspend fun register(login: String, password: String): Result<RegisterResponse>
    suspend fun clearAuthData()
    suspend fun googleLogin(idToken: String): Result<LoginResponse>
    fun getJwtToken(): String?
}

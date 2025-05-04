package by.andros.lea_front.auth.domain

import by.andros.lea_front.auth.data.AuthApiService
import by.andros.lea_front.auth.data.LoginRequest
import by.andros.lea_front.auth.data.LoginResponse
import by.andros.lea_front.auth.data.RegisterRequest
import by.andros.lea_front.auth.data.RegisterResponse
import jakarta.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService
) : AuthRepository {
    
    override suspend fun login(login: String, password: String): Result<LoginResponse> {
        try {
            val response = authApiService.login(LoginRequest(login, password))
            return Result.success((response.body()!!))
        }
        catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    override suspend fun register(login: String, password: String): Result<RegisterResponse> {
        try {
            val response = authApiService.register(RegisterRequest(login, password, "user"))
            return Result.success((response.body()!!))
        }
        catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
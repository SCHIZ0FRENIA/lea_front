package by.andros.lea_front.auth.domain

import by.andros.lea_front.auth.data.AuthApiService
import by.andros.lea_front.auth.data.LoginRequest
import retrofit2.HttpException
import java.io.IOException

class AuthRepositoryImpl(
    private val authApiService: AuthApiService
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = authApiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: IOException) {
            Result.failure(IOException("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Login failed: ${e.message}"))
        }
    }
}
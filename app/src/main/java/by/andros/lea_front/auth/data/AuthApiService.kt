package by.andros.lea_front.auth.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<Unit>
}
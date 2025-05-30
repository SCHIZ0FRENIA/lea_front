package by.andros.lea_front.auth.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AuthApiService {
    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>
    
    @POST("v1/auth/register/admin")
    suspend fun registerAdmin(
        @Header("Authorization") token: String,
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("v1/auth/google-login")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<LoginResponse>
    
    @PUT("v1/auth/users/{userId}/grant-admin")
    suspend fun grantAdminRole(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<UserRoleResponse>
    
    @PUT("v1/auth/users/{userId}/revoke-admin")
    suspend fun revokeAdminRole(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<UserRoleResponse>
    
    @DELETE("v1/auth/users/{userId}")
    suspend fun deleteUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<DeleteUserResponse>
    
    @GET("v1/auth/users")
    suspend fun getAllUsers(
        @Header("Authorization") token: String
    ): Response<UserListResponse>
}
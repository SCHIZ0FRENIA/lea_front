package by.andros.lea_front.auth.domain

import by.andros.lea_front.auth.data.LoginResponse
import by.andros.lea_front.auth.data.RegisterResponse
import by.andros.lea_front.auth.data.UserDto
import by.andros.lea_front.auth.data.UserListResponse
import by.andros.lea_front.auth.data.UserRoleResponse
import by.andros.lea_front.auth.data.DeleteUserResponse

interface AuthRepository {
    suspend fun login(login: String, password: String): Result<LoginResponse>
    suspend fun register(login: String, password: String): Result<RegisterResponse>
    suspend fun registerAdmin(login: String, password: String): Result<RegisterResponse>
    suspend fun clearAuthData()
    suspend fun googleLogin(idToken: String): Result<LoginResponse>
    fun getJwtToken(): String?
    fun getUserLogin(): String?
    fun isUserAdmin(): Boolean
    suspend fun grantAdminRole(userId: String): Result<UserRoleResponse>
    suspend fun revokeAdminRole(userId: String): Result<UserRoleResponse>
    suspend fun getAllUsers(): Result<List<UserDto>>
    suspend fun deleteUser(userId: String): Result<DeleteUserResponse>
}

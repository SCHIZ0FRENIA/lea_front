package by.andros.lea_front.auth.domain

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import by.andros.lea_front.auth.data.AuthApiService
import by.andros.lea_front.auth.data.LoginRequest
import by.andros.lea_front.auth.data.LoginResponse
import by.andros.lea_front.auth.data.RegisterRequest
import by.andros.lea_front.auth.data.RegisterResponse
import by.andros.lea_front.auth.data.UserDto
import by.andros.lea_front.auth.data.UserListResponse
import by.andros.lea_front.auth.data.UserRoleResponse
import by.andros.lea_front.auth.data.DeleteUserResponse
import by.andros.lea_front.auth.data.GoogleLoginRequest
import by.andros.lea_front.auth.service.JwtService
import com.google.gson.JsonParser
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val sharedPreferences: SharedPreferences,
    private val jwtService: JwtService
) : AuthRepository {

    override fun getJwtToken(): String? {
        return sharedPreferences.getString("jwt", null)
    }
    
    override fun getUserLogin(): String? {
        return sharedPreferences.getString("login", null)
    }
    
    override fun isUserAdmin(): Boolean {
        val token = getJwtToken() ?: return false
        return jwtService.isUserAdmin(token)
    }

    override suspend fun login(login: String, password: String): Result<LoginResponse> {
        try {
            val response = authApiService.login(LoginRequest(login, password))
            if (!response.isSuccessful) {
                // Try to parse error body for a message
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val json = JsonParser.parseString(errorBody).asJsonObject
                    json["message"]?.asString ?: "Login failed. Please check your credentials."
                } catch (e: Exception) {
                    errorBody ?: "Login failed. Please check your credentials."
                }
                return Result.failure(Exception(errorMessage))
            }
            val token = response.body()!!.token

            Log.d("JWT_TOKEN_RECEIVED", token)

            // Use JwtService to parse token
            val identityClaims = jwtService.parseToken(token)

            if (identityClaims == null) {
                Log.e("AUTH_ERROR", "JWT parsing failed")
                return Result.failure(IllegalStateException("JWT parsing failed"))
            }

            val userLogin = identityClaims["login"] as? String
            if (userLogin == null) {
                Log.e("AUTH_ERROR", "Login not found in JWT claims")
                return Result.failure(IllegalStateException("Login information missing from token"))
            }

            // Get role either from token claim or response body
            val role = identityClaims["role"] as? String ?: response.body()!!.role

            sharedPreferences.edit(true) {
                putString("jwt", token)
                putString("role", role)
                putString("login", userLogin)
            }
            return Result.success((response.body()!!))
        } catch (e: Exception) {
            Log.e("AUTH_GLOBAL_ERROR", "Error during login: ${e.message}", e)
            return Result.failure(e)
        }
    }

    override suspend fun register(login: String, password: String): Result<RegisterResponse> {
        return try {
            // Regular users always get the "user" role
            val registerResponse = authApiService.register(RegisterRequest(login, password, "user"))
            handleRegisterResponse(registerResponse)
        } catch (e: Exception) {
            Log.e("AUTH_GLOBAL_ERROR_REG", e.toString())
            Result.failure(e)
        }
    }
    
    override suspend fun registerAdmin(login: String, password: String): Result<RegisterResponse> {
        return try {
            // Check if the current user is admin
            if (!isUserAdmin()) {
                return Result.failure(SecurityException("Only admins can create admin accounts"))
            }
            
            val token = getJwtToken() ?: return Result.failure(SecurityException("Authentication required"))
            
            // Create an admin user with "admin" role
            val registerResponse = authApiService.registerAdmin(
                "Bearer $token", 
                RegisterRequest(login, password, "admin")
            )
            handleRegisterResponse(registerResponse)
        } catch (e: Exception) {
            Log.e("AUTH_GLOBAL_ERROR_REG_ADMIN", e.toString())
            Result.failure(e)
        }
    }
    
    private suspend fun handleRegisterResponse(registerResponse: retrofit2.Response<RegisterResponse>): Result<RegisterResponse> {
        if (registerResponse.isSuccessful) {
            val token = registerResponse.body()!!.token
            val role = registerResponse.body()!!.role

            Log.d("JWT_TOKEN_RECEIVED_REG", token)

            // Use JwtService to parse token
            val identityClaims = jwtService.parseToken(token)

            if (identityClaims == null) {
                Log.e("AUTH_ERROR_REG", "JWT parsing failed in registration")
                return Result.failure(IllegalStateException("JWT parsing failed in registration"))
            }

            val userLogin = identityClaims["login"] as? String
            if (userLogin == null) {
                Log.e("AUTH_ERROR_REG", "Login not found in JWT claims in registration")
                return Result.failure(IllegalStateException("Login information missing from token in registration"))
            }

            Log.d("JWT_IDENTITY_CLAIM_PARSED_REG", "Login: $userLogin, Role: ${identityClaims["role"]}")

            sharedPreferences.edit(true) {
                putString("jwt", token)
                putString("role", role)
                putString("login", userLogin)
            }
            return Result.success(registerResponse.body()!!)
        } else {
            return Result.failure(Exception(registerResponse.errorBody()?.string() ?: "Registration failed"))
        }
    }

    override suspend fun clearAuthData() {
        sharedPreferences.edit {
            remove("jwt")
            remove("role")
            remove("login")
        }
        Log.d("AUTH", "Authentication data cleared from SharedPreferences")
    }

    override suspend fun googleLogin(idToken: String): Result<LoginResponse> {
        // TODO: Implement actual Google Sign-In logic here.
        // This typically involves sending the idToken to your backend for verification
        // and then your backend returning a session token or user details.
        // For now, this is a placeholder.

        Log.d("GoogleLogin", "Attempting Google login with ID Token: $idToken")

        return try {
            // Simulate a successful login for demonstration
            // In a real app, you would make a network call to your backend with the idToken
            // val response = authApiService.googleLogin(GoogleLoginRequest(idToken))
            // if (response.isSuccessful) { ... } else { ... }

            // Placeholder: Assume success and return a dummy LoginResponse
            val dummyToken = "dummy_jwt_for_google_user"
            val dummyRole = "user"
            val dummyLogin = "google_user_${System.currentTimeMillis()}" // Simulate a unique login for the user

            sharedPreferences.edit(true) {
                putString("jwt", dummyToken)
                putString("role", dummyRole)
                putString("login", dummyLogin)
            }
            Result.success(LoginResponse("Google login successful (placeholder)", dummyToken, dummyRole))
        } catch (e: Exception) {
            Log.e("AUTH_GLOBAL_ERROR_GOOGLE_LOGIN", e.toString())
            Result.failure(e)
        }
    }
    
    override suspend fun grantAdminRole(userId: String): Result<UserRoleResponse> {
        return try {
            // Check if the current user is admin
            if (!isUserAdmin()) {
                return Result.failure(SecurityException("Only admins can grant admin privileges"))
            }
            
            val token = getJwtToken() ?: return Result.failure(SecurityException("Authentication required"))
            
            val response = authApiService.grantAdminRole("Bearer $token", userId)
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val json = JsonParser.parseString(errorBody).asJsonObject
                    json["message"]?.asString ?: "Failed to grant admin role"
                } catch (e: Exception) {
                    errorBody ?: "Failed to grant admin role"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("AUTH_ADMIN_GRANT", "Error granting admin role: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun revokeAdminRole(userId: String): Result<UserRoleResponse> {
        return try {
            // Check if the current user is admin
            if (!isUserAdmin()) {
                return Result.failure(SecurityException("Only admins can revoke admin privileges"))
            }
            
            val token = getJwtToken() ?: return Result.failure(SecurityException("Authentication required"))
            
            val response = authApiService.revokeAdminRole("Bearer $token", userId)
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val json = JsonParser.parseString(errorBody).asJsonObject
                    json["message"]?.asString ?: "Failed to revoke admin role"
                } catch (e: Exception) {
                    errorBody ?: "Failed to revoke admin role"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("AUTH_ADMIN_REVOKE", "Error revoking admin role: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getAllUsers(): Result<List<UserDto>> {
        return try {
            // Check if the current user is admin
            if (!isUserAdmin()) {
                return Result.failure(SecurityException("Only admins can view all users"))
            }
            
            val token = getJwtToken() ?: return Result.failure(SecurityException("Authentication required"))
            
            val response = authApiService.getAllUsers("Bearer $token")
            
            if (response.isSuccessful) {
                Result.success(response.body()!!.users)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val json = JsonParser.parseString(errorBody).asJsonObject
                    json["message"]?.asString ?: "Failed to get users"
                } catch (e: Exception) {
                    errorBody ?: "Failed to get users"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("AUTH_GET_USERS", "Error getting users: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(userId: String): Result<DeleteUserResponse> {
        return try {
            // Check if the current user is admin
            if (!isUserAdmin()) {
                return Result.failure<DeleteUserResponse>(SecurityException("Only admins can delete users"))
            }
            
            val token = getJwtToken() ?: return Result.failure<DeleteUserResponse>(SecurityException("Authentication required"))
            
            val response = authApiService.deleteUser("Bearer $token", userId)
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val json = JsonParser.parseString(errorBody).asJsonObject
                    json["message"]?.asString ?: "Failed to delete user"
                } catch (e: Exception) {
                    errorBody ?: "Failed to delete user"
                }
                Result.failure<DeleteUserResponse>(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("AUTH_DELETE_USER", "Error deleting user: ${e.message}", e)
            Result.failure<DeleteUserResponse>(e)
        }
    }
}

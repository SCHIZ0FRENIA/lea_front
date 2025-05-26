package by.andros.lea_front.auth.domain

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import by.andros.lea_front.auth.data.AuthApiService
import by.andros.lea_front.auth.data.LoginRequest
import by.andros.lea_front.auth.data.LoginResponse
import by.andros.lea_front.auth.data.RegisterRequest
import by.andros.lea_front.auth.data.RegisterResponse
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import javax.inject.Inject

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.security.Key

class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val sharedPreferences: SharedPreferences
) : AuthRepository {

    private val FLASK_SECRET_KEY = "JKSDHVUPINJEWIUVH:DSNVBOKKJ!@#J!@#()IJFDNSNV*#(@RHFVJDN(*#EINDV)#@()IFJIVDSO"

    override fun getJwtToken(): String? {
        return sharedPreferences.getString("jwt", null)
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

            val secretKey: Key = Keys.hmacShaKeyFor(FLASK_SECRET_KEY.toByteArray(StandardCharsets.UTF_8))

            val claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .setAllowedClockSkewSeconds(5)
                .build()
                .parseClaimsJws(token)
                .body

            val identityClaims = claims["sub"] as? Map<*, *>

            if (identityClaims == null) {
                Log.e("AUTH_ERROR", "JWT 'sub' claim is null or not a map after JJWT parsing")
                return Result.failure(IllegalStateException("JWT 'sub' claim missing or malformed"))
            }

            val userLogin = identityClaims["login"] as? String
            if (userLogin == null) {
                Log.e("AUTH_ERROR", "Login not found in 'sub' claim")
                return Result.failure(IllegalStateException("Login information missing from token"))
            }

            Log.d("JWT_IDENTITY_CLAIM_PARSED", "Login: $userLogin, User ID: ${identityClaims["user_id"]}, Role: ${identityClaims["role"]}")

            sharedPreferences.edit(true) {
                putString("jwt", token)
                putString("role", response.body()!!.role)
                putString("login", userLogin)
            }
            return Result.success((response.body()!!))
        } catch (e: Exception) {
            Log.e("AUTH_GLOBAL_ERROR", "Error during login with JJWT and Gson: ${e.message}", e)
            return Result.failure(e)
        }
    }

    override suspend fun register(login: String, password: String): Result<RegisterResponse> {
        return try {
            val registerResponse = authApiService.register(RegisterRequest(login, password, "user"))
            if (registerResponse.isSuccessful) {
                val token = registerResponse.body()!!.token
                val role = registerResponse.body()!!.role

                Log.d("JWT_TOKEN_RECEIVED_REG", token)

                val secretKey: Key = Keys.hmacShaKeyFor(FLASK_SECRET_KEY.toByteArray(StandardCharsets.UTF_8))

                val claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .setAllowedClockSkewSeconds(5)
                    .build()
                    .parseClaimsJws(token)
                    .body

                val identityClaims = claims["sub"] as? Map<*, *>

                if (identityClaims == null) {
                    Log.e("AUTH_ERROR_REG", "JWT 'sub' claim is null or not a map after JJWT parsing in registration")
                    return Result.failure(IllegalStateException("JWT 'sub' claim missing or malformed in registration"))
                }

                val userLogin = identityClaims["login"] as? String
                if (userLogin == null) {
                    Log.e("AUTH_ERROR_REG", "Login not found in 'sub' claim in registration")
                    return Result.failure(IllegalStateException("Login information missing from token in registration"))
                }

                Log.d("JWT_IDENTITY_CLAIM_PARSED_REG", "Login: $userLogin, Role: ${identityClaims["role"]}")

                sharedPreferences.edit(true) {
                    putString("jwt", token)
                    putString("role", role)
                    putString("login", userLogin)
                }
                Result.success(registerResponse.body()!!)
            } else {
                Result.failure(Exception(registerResponse.errorBody()?.string() ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Log.e("AUTH_GLOBAL_ERROR_REG", e.toString())
            Result.failure(e)
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
}

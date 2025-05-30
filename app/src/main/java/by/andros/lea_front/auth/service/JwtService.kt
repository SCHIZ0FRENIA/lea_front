package by.andros.lea_front.auth.service

import android.util.Log
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.security.Key
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JwtService @Inject constructor() {
    // This should be synchronized with the backend secret key
    private val FLASK_SECRET_KEY = "JKSDHVUPINJEWIUVH:DSNVBOKKJ!@#J!@#()IJFDNSNV*#(@RHFVJDN(*#EINDV)#@()IFJIVDSO"
    
    private val secretKey: Key by lazy {
        Keys.hmacShaKeyFor(FLASK_SECRET_KEY.toByteArray(StandardCharsets.UTF_8))
    }
    
    fun parseToken(token: String): Map<String, Any?>? {
        return try {
            Log.d("JWT_SERVICE", "Attempting to parse token: ${token.take(20)}...")
            
            val claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .setAllowedClockSkewSeconds(5)
                .build()
                .parseClaimsJws(token)
                .body
            
            // Log all claims for debugging
            logAllClaims(claims)
            
            // Create a result map from the claims
            val result = mutableMapOf<String, Any?>()
            
            // Add the user ID from the subject claim
            result["id"] = claims["sub"] as? String
            
            // Extract login and role directly from claims
            result["login"] = claims["login"] as? String
            result["role"] = claims["role"] as? String
            
            // Log the claims for debugging
            Log.d("JWT_SERVICE", "Parsed claims: sub=${claims["sub"]}, login=${claims["login"]}, role=${claims["role"]}")
            
            if (result["login"] == null || result["role"] == null) {
                Log.e("JWT_SERVICE", "JWT missing required claims: login=${result["login"]}, role=${result["role"]}")
                null
            } else {
                result
            }
        } catch (e: Exception) {
            Log.e("JWT_SERVICE", "Error parsing token: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    private fun logAllClaims(claims: Claims) {
        try {
            Log.d("JWT_SERVICE", "All JWT claims:")
            claims.entries.forEach { entry ->
                Log.d("JWT_SERVICE", "  ${entry.key}: ${entry.value}")
            }
        } catch (e: Exception) {
            Log.e("JWT_SERVICE", "Error logging claims: ${e.message}")
        }
    }
    
    fun isTokenValid(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .setAllowedClockSkewSeconds(5)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            Log.e("JWT_SERVICE", "Invalid token: ${e.message}")
            false
        }
    }
    
    fun getUserRole(token: String): String? {
        try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .setAllowedClockSkewSeconds(5)
                .build()
                .parseClaimsJws(token)
                .body
                
            return claims["role"] as? String
        } catch (e: Exception) {
            Log.e("JWT_SERVICE", "Error getting user role: ${e.message}", e)
            return null
        }
    }
    
    fun getUserLogin(token: String): String? {
        try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .setAllowedClockSkewSeconds(5)
                .build()
                .parseClaimsJws(token)
                .body
                
            return claims["login"] as? String
        } catch (e: Exception) {
            Log.e("JWT_SERVICE", "Error getting user login: ${e.message}", e)
            return null
        }
    }
    
    fun isUserAdmin(token: String): Boolean {
        return getUserRole(token) == "admin"
    }
} 
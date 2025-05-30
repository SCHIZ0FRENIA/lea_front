package by.andros.lea_front.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global theme manager to ensure consistent theme application across all screens
 */
object ThemeManager {
    // Theme options
    enum class Theme {
        LIGHT, DARK, SYSTEM
    }
    
    private lateinit var sharedPreferences: SharedPreferences
    private val _currentTheme = MutableStateFlow<Theme?>(null)
    val currentTheme: StateFlow<Theme?> = _currentTheme.asStateFlow()
    
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "theme") {
            updateThemeFromPreferences()
        }
    }
    
    fun initialize(context: Context) {
        if (!::sharedPreferences.isInitialized) {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener)
            updateThemeFromPreferences()
        }
    }
    
    private fun updateThemeFromPreferences() {
        val themeString = sharedPreferences.getString("theme", "system") ?: "system"
        _currentTheme.value = when (themeString) {
            "light" -> Theme.LIGHT
            "dark" -> Theme.DARK
            else -> Theme.SYSTEM
        }
    }
    
    fun setTheme(theme: Theme) {
        val themeString = when (theme) {
            Theme.LIGHT -> "light"
            Theme.DARK -> "dark"
            Theme.SYSTEM -> "system"
        }
        sharedPreferences.edit().putString("theme", themeString).apply()
        // The preference listener will update the flow
    }
    
    @Composable
    fun currentThemeAsState(): State<Theme?> {
        return currentTheme.collectAsState()
    }
} 
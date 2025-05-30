package by.andros.lea_front

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import by.andros.lea_front.theme.Android_frontTheme
import by.andros.lea_front.theme.PREFS_NAME
import by.andros.lea_front.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import android.os.Build
import android.provider.Settings
import android.content.Intent
import android.os.Environment
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize theme manager
        ThemeManager.initialize(this)

        // Request storage permissions if needed
        requestStoragePermissions()

        setContent {
            val currentTheme by ThemeManager.currentThemeAsState()
            
            // Convert ThemeManager.Theme to Boolean for Android_frontTheme
            val isDarkTheme = when (currentTheme) {
                ThemeManager.Theme.DARK -> true
                ThemeManager.Theme.LIGHT -> false
                ThemeManager.Theme.SYSTEM, null -> null
            }
            
            Android_frontTheme(darkTheme = isDarkTheme) {
                AppNavigation()
            }
        }
    }
    
    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+): Request MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:" + packageName)
                startActivity(intent)
            }
        } else {
            // Below Android 11: Request legacy permissions
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val permissionsToRequest = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 100)
            }
        }
    }
}
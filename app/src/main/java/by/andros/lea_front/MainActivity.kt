package by.andros.lea_front

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import by.andros.lea_front.auth.presentation.login.LoginScreen
import by.andros.android_front.ui.theme.Android_frontTheme

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Android_frontTheme {
                LoginScreen()
            }
        }
    }
    
}
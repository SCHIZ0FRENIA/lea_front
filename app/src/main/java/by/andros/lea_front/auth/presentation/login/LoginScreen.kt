package by.andros.lea_front.auth.presentation.login

import android.content.Intent
import android.net.Uri
import android.util.Log // Added import for Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToRegistration: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Launcher for Custom Tabs (browser)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            // This callback is for when the Custom Tab activity finishes.
            // The OIDC redirect will be handled by the deep link in AndroidManifest.xml
            // and passed to MainActivity, then to ViewModel.
            // This specific launcher is primarily for initiating the browser.
            // We don't expect a direct result here for the auth code.
        }
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.Navigation.ToHome -> onNavigateToHome()
                is LoginEvent.Navigation.ToRegistration -> onNavigateToRegistration()
                is LoginEvent.Navigation.LaunchGoogleSignIn -> {
                    // Construct the Google OAuth URL
                    val clientId = "YOUR_GOOGLE_ANDROID_CLIENT_ID" // Replace with your Android Client ID
                    val redirectUri = "leafront://oauth2redirect" // Must match your AndroidManifest.xml and Google Console
                    val scope = "openid profile email" // Request necessary scopes
                    val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                            "client_id=$clientId&" +
                            "redirect_uri=$redirectUri&" +
                            "response_type=code&" + // Request an authorization code
                            "scope=$scope&" +
                            "access_type=offline&" + // To get a refresh token
                            "prompt=consent" // To ensure consent screen is shown
                    Log.d("OIDC", "Launching Google Sign-In URL: $authUrl")

                    val customTabsIntent = CustomTabsIntent.Builder().build()
                    customTabsIntent.launchUrl(context, Uri.parse(authUrl))
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 58.dp,
            ),
        verticalArrangement = Arrangement.Center
    ) {

        OutlinedTextField(
            value = state.login,
            onValueChange = { viewModel.onEvent(LoginEvent.LoginChanged(it)) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (!state.isLoading && !state.isSuccess){
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        state.error?.let { error ->
            Text(
                text = error,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                color = Color.Red
            )
        }

        if (state.isSuccess) {
            Text(
                text = "Login Successful!",
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        ElevatedButton(
            onClick = { viewModel.onEvent(LoginEvent.Submit) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            Text("Login")
        }

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            FilledIconButton(
                onClick = {
                    // Trigger the Google Sign-In flow
                    viewModel.onEvent(LoginEvent.GoogleSignIn(authCode = null))
                }
            ) {
                Icon(Icons.Filled.AccountCircle, "Sign in with Google")
            }
        }

        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton (
                onClick = {
                    onNavigateToRegistration()
                },
                enabled = !state.isLoading
            ) {
                Text("Sign up")
            }

            Spacer(modifier = Modifier.width(12.dp))

            TextButton (
                onClick = {
                    onNavigateToHome()
                },
                enabled = !state.isLoading
            ) {
                Text("Continue without account")
            }
        }
    }
}

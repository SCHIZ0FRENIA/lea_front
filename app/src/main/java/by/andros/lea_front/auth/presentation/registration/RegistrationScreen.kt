package by.andros.lea_front.auth.presentation.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import by.andros.lea_front.auth.presentation.login.LoginEvent

@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RegistrationEvent.Navigation.ToHome -> onNavigateToHome()
                else -> {}
            }
        }
    }
    
    // Wrap the entire screen in a Surface that uses MaterialTheme's background color
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                onValueChange = { viewModel.onEvent(RegistrationEvent.LoginChanged(it)) },
                label = { Text("Login") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(RegistrationEvent.PasswordChanged(it)) },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = state.confirmation,
                onValueChange = { viewModel.onEvent(RegistrationEvent.ConfirmationChanged(it)) },
                label = { Text("Confirmation") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            
            if (!state.isLoading and !state.isSuccess) {
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
                onClick = { viewModel.onEvent(RegistrationEvent.Register) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                Text("Register")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        onNavigateToLogin()
                    },
                    enabled = !state.isLoading
                ) {
                    Text("Sign in")
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                TextButton(
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
}
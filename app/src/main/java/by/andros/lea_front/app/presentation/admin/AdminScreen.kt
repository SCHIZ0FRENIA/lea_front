package by.andros.lea_front.app.presentation.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import by.andros.lea_front.auth.data.UserDto
import kotlinx.coroutines.launch
import by.andros.lea_front.theme.PREFS_NAME

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    onLogout: () -> Unit,
    onNavigateToPublicDecks: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val adminName = prefs.getString("login", "Admin") ?: "Admin"
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AdminEvent.Navigation.ToLogin -> onLogout()
            }
        }
    }
    
    // Show success or error messages
    LaunchedEffect(state.successMessage, state.error) {
        state.successMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.onEvent(AdminEvent.ClearMessages)
            }
        }
        
        state.error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.onEvent(AdminEvent.ClearMessages)
            }
        }
    }
    
    // User list dialog
    if (state.isUserListDialogVisible) {
        UserListDialog(
            users = state.users,
            onDismiss = { viewModel.onEvent(AdminEvent.HideUserListDialog) },
            onUserIdClick = { userId ->
                // Copy user ID to clipboard
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("User ID", userId)
                clipboard.setPrimaryClip(clip)
                
                // Show toast notification
                Toast.makeText(context, "User ID copied to clipboard", Toast.LENGTH_SHORT).show()
                
                // Update the userIdToManage field
                viewModel.onEvent(AdminEvent.CopyUserIdToClipboard(userId))
            },
            onDeleteUser = { user ->
                viewModel.onEvent(AdminEvent.ShowDeleteUserConfirmation(user))
            }
        )
    }
    
    // Delete confirmation dialog
    if (state.showDeleteConfirmDialog && state.userToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onEvent(AdminEvent.HideDeleteUserConfirmation)
            },
            title = { Text("Confirm User Deletion") },
            text = { 
                Text("Are you sure you want to delete user '${state.userToDelete!!.login}'? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(AdminEvent.DeleteUser(state.userToDelete!!.userId))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(AdminEvent.HideDeleteUserConfirmation)
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF8B5CF6),
                    titleContentColor = Color.White
                ),
                actions = {
                    // User list button
                    IconButton(onClick = { viewModel.onEvent(AdminEvent.ShowUserListDialog) }) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = "User List",
                            tint = Color.White
                        )
                    }
                    
                    // Public Decks button
                    IconButton(onClick = onNavigateToPublicDecks) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = "Manage Public Decks",
                            tint = Color.White
                        )
                    }
                    
                    // Logout button
                    IconButton(onClick = { viewModel.onEvent(AdminEvent.Logout) }) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Welcome, $adminName",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                
                // Create New Admin Account
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Create Admin Account",
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        OutlinedTextField(
                            value = state.newAdminLogin,
                            onValueChange = { viewModel.onEvent(AdminEvent.UpdateNewAdminLogin(it)) },
                            label = { Text("Admin Username") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = state.newAdminPassword,
                            onValueChange = { viewModel.onEvent(AdminEvent.UpdateNewAdminPassword(it)) },
                            label = { Text("Admin Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Button(
                            onClick = { 
                                viewModel.onEvent(
                                    AdminEvent.CreateAdminAccount(
                                        state.newAdminLogin, 
                                        state.newAdminPassword
                                    )
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading && state.newAdminLogin.isNotBlank() && state.newAdminPassword.isNotBlank()
                        ) {
                            Text("Create Admin Account")
                        }
                    }
                }
                
                // Public Decks Management
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Public Decks Management",
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        Text(
                            text = "Manage community decks and moderate content",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Button(
                            onClick = onNavigateToPublicDecks,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manage Public Decks")
                        }
                        
                        Text(
                            text = "As an admin, you can view, edit, and delete any public deck. You will have access to additional options not available to regular users.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Manage User Roles
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Manage User Roles",
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        OutlinedTextField(
                            value = state.userIdToManage,
                            onValueChange = { viewModel.onEvent(AdminEvent.UpdateUserIdToManage(it)) },
                            label = { Text("User ID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Button(
                            onClick = { viewModel.onEvent(AdminEvent.ShowUserListDialog) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View User List")
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.onEvent(AdminEvent.GrantAdminRole(state.userIdToManage)) },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isLoading && state.userIdToManage.isNotBlank()
                            ) {
                                Text("Grant Admin")
                            }
                            
                            Button(
                                onClick = { viewModel.onEvent(AdminEvent.RevokeAdminRole(state.userIdToManage)) },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isLoading && state.userIdToManage.isNotBlank()
                            ) {
                                Text("Revoke Admin")
                            }
                        }
                    }
                }
                
                // System Status
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "System Status",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Everything is running smoothly",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = { viewModel.onEvent(AdminEvent.Logout) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Logout")
                }
            }
        }
    }
}

@Composable
fun UserListDialog(
    users: List<UserDto>,
    onDismiss: () -> Unit,
    onUserIdClick: (String) -> Unit,
    onDeleteUser: (UserDto) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User List") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                items(users) { user ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "${user.login} (${user.role})",
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ID: ${user.userId}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                IconButton(
                                    onClick = { onUserIdClick(user.userId) }
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy ID",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            // Delete button
                            IconButton(
                                onClick = { onDeleteUser(user) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete user",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
} 
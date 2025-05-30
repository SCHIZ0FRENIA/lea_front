package by.andros.lea_front.app.presentation.publicdecks

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicDecksScreen(
    onNavigateBack: () -> Unit,
    viewModel: PublicDecksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var deckToDelete by remember { mutableStateOf<String?>(null) }
    
    // Show snackbar when message or error changes
    LaunchedEffect(state.message, state.error) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(PublicDecksEvent.ClearMessage)
        }
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(PublicDecksEvent.ClearMessage)
        }
    }
    
    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is PublicDecksEvent.Navigation.GoToHome -> {
                    // Navigate back to the home page after downloading a deck
                    onNavigateBack()
                }
            }
        }
    }
    
    // Handle session expiration
    var showSessionExpiredDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) {
            showSessionExpiredDialog = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Public Decks") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { query ->
                    viewModel.onEvent(PublicDecksEvent.SearchDecks(query))
                },
                onSearch = { query ->
                    viewModel.onEvent(PublicDecksEvent.SearchDecks(query))
                },
                active = false,
                onActiveChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search decks...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.onEvent(PublicDecksEvent.SearchDecks(""))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear"
                            )
                        }
                    }
                }
            ) { }
            
            // Loading indicator
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.decks.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.searchQuery.isEmpty()) 
                            "No public decks available" 
                        else 
                            "No results found for '${state.searchQuery}'",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // Decks list
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.decks) { deck ->
                        DeckItem(
                            deck = deck,
                            isAdmin = state.isAdmin,
                            onDownload = {
                                viewModel.onEvent(PublicDecksEvent.DownloadDeck(deck.id))
                            },
                            onDelete = {
                                deckToDelete = deck.id
                                showConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Confirmation dialog for deletion
    if (showConfirmDialog && deckToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                deckToDelete = null
            },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete this deck? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        deckToDelete?.let { deckId ->
                            viewModel.onEvent(PublicDecksEvent.DeleteDeck(deckId))
                        }
                        showConfirmDialog = false
                        deckToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        deckToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add session expired dialog
    if (showSessionExpiredDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSessionExpiredDialog = false
                viewModel.onEvent(PublicDecksEvent.ClearMessage)
            },
            title = { Text("Session Expired") },
            text = { Text("Your login session has expired. Please log in again to continue.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSessionExpiredDialog = false
                        viewModel.onEvent(PublicDecksEvent.ClearMessage)
                        onNavigateBack()
                    }
                ) {
                    Text("Go to Login")
                }
            }
        )
    }
}

@Composable
fun DeckItem(
    deck: by.andros.lea_front.app.data.model.PublicDeckResponse,
    isAdmin: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = deck.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            deck.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Creator info
                Text(
                    text = "Created by: ${deck.createdBy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Actions
                Row {
                    // Download button
                    IconButton(onClick = onDownload) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download deck",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Delete button (only for admins)
                    if (isAdmin) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete deck",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
} 
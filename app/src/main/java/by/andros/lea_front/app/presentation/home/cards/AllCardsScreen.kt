package by.andros.lea_front.app.presentation.home.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import by.andros.lea_front.app.data.Card
import by.andros.lea_front.app.data.Deck
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCardsScreen(
    deckId: Long,
    viewModel: AllCardsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCardDetail: (Long) -> Unit
) {
    val stateFlow = viewModel.state
    val state by stateFlow.collectAsState()
    var showNewCardDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<Card?>(null) }
    var cardToDelete by remember { mutableStateOf<Card?>(null) }
    val context = LocalContext.current
    var exportUri by remember { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) {
            exportUri = uri
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.onEvent(AllCardsEvent.ImportDeck(uri))
        }
    }

    // Handle navigation events
    LaunchedEffect(state) {
        if (state is AllCardsState.Loaded) {
            val loadedState = state as AllCardsState.Loaded
            if (loadedState.navigationEvents is AllCardsNavigationEvent.NavigateBack) {
                onNavigateBack()
                viewModel.onEvent(AllCardsEvent.ClearNavigationEvent) // Reset event
            }
        }
    }

    LaunchedEffect(exportUri) {
        val uri = exportUri
        if (uri != null) {
            val currentState = state as? AllCardsState.Loaded ?: return@LaunchedEffect
            val deck = currentState.deck ?: return@LaunchedEffect
            val cards = currentState.cards
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val sb = StringBuilder()
                    sb.appendLine("# Deck name")
                    sb.appendLine(deck.name)
                    sb.appendLine("# Deck description")
                    sb.appendLine(deck.description ?: "")
                    for (card in cards) {
                        sb.appendLine("## ${card.front}")
                        sb.appendLine("### Front")
                        sb.appendLine(card.front)
                        sb.appendLine("### Back")
                        sb.appendLine(card.back)
                    }
                    outputStream.write(sb.toString().toByteArray())
                }
                Toast.makeText(context, "Deck exported!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            exportUri = null
        }
    }

    Scaffold(
        topBar = {
            val currentDeck = (state as? AllCardsState.Loaded)?.deck
            val isUserLoggedIn = (state as? AllCardsState.Loaded)?.isUserLoggedIn ?: false

            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentDeck?.name ?: "Deck Details",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!currentDeck?.description.isNullOrBlank()) {
                            Text(
                                text = currentDeck?.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("${currentDeck?.name ?: "deck"}.md") }) {
                        Icon(Icons.Filled.Save, contentDescription = "Export Deck")
                    }
                    // Edit Button
                    IconButton(onClick = { viewModel.onEvent(AllCardsEvent.ShowEditDeckDialog) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Deck")
                    }
                    // Delete Button
                    IconButton(onClick = { viewModel.onEvent(AllCardsEvent.DeleteDeck) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Deck")
                    }
                    // Publish Button (conditionally displayed)
                    if (isUserLoggedIn) {
                        IconButton(onClick = { viewModel.onEvent(AllCardsEvent.PublishDeck) }) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = "Publish Deck")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewCardDialog = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add new card")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            when (val currentState = state) {
                is AllCardsState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is AllCardsState.Loaded -> {
                    if (currentState.cards.isEmpty()) {
                        Text("No cards found in this deck.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentState.cards) { card ->
                                CardItem(
                                    card = card,
                                    onCardClick = { onNavigateToCardDetail(card.id ?: 0) },
                                    onEditClick = { cardToEdit = card },
                                    onDeleteClick = { cardToDelete = card }
                                )
                            }
                        }
                    }

                    // Edit Deck Dialog
                    if (currentState.showEditDeckDialog) {
                        EditDeckDialog(
                            deck = currentState.deck,
                            onDismiss = { viewModel.onEvent(AllCardsEvent.HideEditDeckDialog) },
                            onConfirm = { name, description ->
                                viewModel.onEvent(AllCardsEvent.UpdateDeck(name, description))
                            }
                        )
                    }

                    // Delete Confirmation Dialog
                    if (currentState.showDeleteConfirmation) {
                        DeleteConfirmDialog(
                            deckName = currentState.deck?.name ?: "this deck",
                            onDismiss = { viewModel.onEvent(AllCardsEvent.CancelDeleteDeck) },
                            onConfirm = { viewModel.onEvent(AllCardsEvent.ConfirmDeleteDeck) }
                        )
                    }

                    // Publish Confirmation Dialog
                    if (currentState.showPublishConfirmation) {
                        PublishConfirmDialog(
                            deckName = currentState.deck?.name ?: "this deck",
                            onDismiss = { viewModel.onEvent(AllCardsEvent.CancelPublishDeck) },
                            onConfirm = { viewModel.onEvent(AllCardsEvent.ConfirmPublishDeck) }
                        )
                    }

                    // Edit Card Dialog
                    cardToEdit?.let { card ->
                        EditCardDialog(
                            card = card,
                            onDismiss = { cardToEdit = null },
                            onConfirm = { newFront, newBack ->
                                viewModel.onEvent(AllCardsEvent.EditCard(card, newFront, newBack))
                                cardToEdit = null
                            }
                        )
                    }

                    // Delete Card Dialog
                    cardToDelete?.let { card ->
                        AlertDialog(
                            onDismissRequest = { cardToDelete = null },
                            title = { Text("Delete Card") },
                            text = { Text("Are you sure you want to delete this card?") },
                            confirmButton = {
                                Button(onClick = {
                                    viewModel.onEvent(AllCardsEvent.DeleteCard(card))
                                    cardToDelete = null
                                }) { Text("Delete") }
                            },
                            dismissButton = {
                                TextButton(onClick = { cardToDelete = null }) { Text("Cancel") }
                            }
                        )
                    }
                }

                is AllCardsState.Error -> {
                    Text("Error: ${currentState.message}")
                }
            }
        }
    }

    if (showNewCardDialog) {
        NewCardDialog(
            onDismiss = { showNewCardDialog = false },
            onCardCreated = { front, back ->
                viewModel.onEvent(AllCardsEvent.AddCard(front, back))
                showNewCardDialog = false
            }
        )
    }
}

@Composable
fun CardItem(
    card: Card,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Front: ${card.front}")
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Back: ${card.back}")
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Card")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Card")
            }
        }
    }
}

@Composable
fun NewCardDialog(
    onDismiss: () -> Unit,
    onCardCreated: (String, String) -> Unit
) {
    var cardFront by remember { mutableStateOf("") }
    var cardBack by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Card") },
        text = {
            Column {
                OutlinedTextField(
                    value = cardFront,
                    onValueChange = { cardFront = it },
                    label = { Text("Front") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cardBack,
                    onValueChange = { cardBack = it },
                    label = { Text("Back") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (cardFront.isNotBlank() && cardBack.isNotBlank()) {
                        onCardCreated(cardFront, cardBack)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditDeckDialog(
    deck: Deck?,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var deckName by remember { mutableStateOf(deck?.name ?: "") }
    var deckDescription by remember { mutableStateOf(deck?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Deck") },
        text = {
            Column {
                OutlinedTextField(
                    value = deckName,
                    onValueChange = { deckName = it },
                    label = { Text("Deck Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deckDescription,
                    onValueChange = { deckDescription = it },
                    label = { Text("Deck Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (deckName.isNotBlank()) {
                        onConfirm(deckName, deckDescription.ifBlank { null })
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    deckName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Deck") },
        text = { Text("Are you sure you want to delete the deck \"$deckName\"? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PublishConfirmDialog(
    deckName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Deck") },
        text = { Text("Are you sure you want to publish the deck \"$deckName\" to the public library? Other users will be able to see and download it.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditCardDialog(
    card: Card,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var front by remember { mutableStateOf(card.front) }
    var back by remember { mutableStateOf(card.back) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Card") },
        text = {
            Column {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (front.isNotBlank() && back.isNotBlank()) {
                        onConfirm(front, back)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
} 
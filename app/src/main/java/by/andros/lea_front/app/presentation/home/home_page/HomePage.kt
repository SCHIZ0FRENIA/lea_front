@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package by.andros.lea_front.app.presentation.home.home_page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import by.andros.lea_front.app.data.Deck
import by.andros.lea_front.app.presentation.components.RowButton
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomePage(
    viewModel: HomePageViewModel = hiltViewModel(),
    onNavigateToShowAllDecks: () -> Unit,
    onNavigateToShowCards: (Long) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(HomePageEvent.LoadDecks)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HomePageEvent.Navigation.ToAllDecks -> onNavigateToShowAllDecks()
                is HomePageEvent.Navigation.ToDeckDetails -> {  }
                is HomePageEvent.Navigation.ToCards -> onNavigateToShowCards(event.deckId)
            }
        }
    }

    Column {
        RowButton(name = "Private",
            buttonText = "Show all",
            onButtonClick = { viewModel.onEvent(HomePageEvent.ShowAllDecks) })

        Spacer(modifier = Modifier.height(16.dp))

        when (val currentState = state) {
            is HomePageState.Loading -> ShowLoading()
            is HomePageState.Loaded -> ShowDecks(currentState.decks, onDeckClick = { deckId ->
                viewModel.onEvent(HomePageEvent.DeckSelected(deckId))
            })
            is HomePageState.Error -> ShowError(currentState.message)
        }
    }
}

@Composable
fun ShowLoading() {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
}

@Composable
fun ShowDecks(
    decks: List<Deck>,
    onDeckClick: (Long) -> Unit
) {
    if (decks.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            for (deck in decks) {
                TextButton(
                    onClick = { deck.id?.let { onDeckClick(it) } },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .width(96.dp)
                        .height(128.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp),
                        )
                        Text(
                            text = deck.name,
                            modifier = Modifier
                                .padding(top = 4.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = true,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    } else {
        Text("There are no decks right now, try creating one!")
    }
}

@Composable
fun ShowError(
    message: String
) {
    Text(
        text = "Something went wrong: $message",
    )
}

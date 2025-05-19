package by.andros.lea_front.app.presentation.home.decks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.andros.lea_front.app.data.Deck


@Composable
fun DeckItem(
    deck: Deck,
    onDeckClick: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { deck.id?.let { onDeckClick(it) } },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row (
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Icon(
                Icons.Default.Book,
                null,
                modifier = Modifier
                    .size(48.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
            ) {
                Text(text = deck.name)
                deck.description?.let {
                    Text(text = it)
                }
            }
        }
    }
}
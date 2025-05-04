package by.andros.lea_front.app.data

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class DeckRepository @Inject constructor(private val deckDao: DeckDao) {
    fun getAllDecks(): Flow<List<Deck>> = deckDao.getAllDecks()
    
    fun getDeckById(deckId: Long): Flow<Deck> = deckDao.getDeckById(deckId)
    
    suspend fun insertDeck(deck: Deck) = deckDao.insertDeck(deck)
    
    suspend fun updateDeck(deck: Deck) = deckDao.updateDeck(deck)
    
    suspend fun deleteDeck(deck: Deck) = deckDao.deleteDeck(deck)
}
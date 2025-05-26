package by.andros.lea_front.app.data

// import by.andros.lea_front.app.data.model.DeckWithCardCount
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
// import kotlinx.coroutines.flow.first
// import kotlinx.coroutines.flow.map

@Singleton
class DeckRepository @Inject constructor(
    private val deckDao: DeckDao,
    private val cardDao: CardDao
) {
    fun getAllDecks(): Flow<List<Deck>> = deckDao.getAllDecks()
    
    fun getCardCountForDeck(deckId: Long): Flow<Int> = cardDao.getCardCountByDeckId(deckId)
    
    fun getDeckById(deckId: Long): Flow<Deck> = deckDao.getDeckById(deckId)
    
    suspend fun insertDeck(deck: Deck) = deckDao.insertDeck(deck)
    
    suspend fun updateDeck(deck: Deck) = deckDao.updateDeck(deck)
    
    suspend fun deleteDeck(deck: Deck) = deckDao.deleteDeck(deck)
}
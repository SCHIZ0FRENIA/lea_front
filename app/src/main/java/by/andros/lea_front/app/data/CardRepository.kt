package by.andros.lea_front.app.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class CardRepository @Inject constructor(private val cardDao: CardDao) {
    fun getCardsByDeck(deckId: Long): Flow<List<Card>> = cardDao.getCardsByDeck(deckId)
    
    fun getCardById(cardId: Long): Flow<Card> = cardDao.getCardById(cardId)
    
    suspend fun insertCard(card: Card) = cardDao.insertCard(card)
    
    suspend fun updateCard(card: Card) = cardDao.updateCard(card)
    
    suspend fun deleteCard(card: Card) = cardDao.deleteCard(card)
}
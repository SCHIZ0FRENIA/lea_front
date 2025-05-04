package by.andros.lea_front.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE deck_id = :deckId ORDER BY next_review ASC")
    fun getCardsByDeck(deckId: Long): Flow<List<Card>>
    
    @Query("SELECT * FROM cards WHERE id = :cardId")
    fun getCardById(cardId: Long): Flow<Card>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card): Long
    
    @Update
    suspend fun updateCard(card: Card)
    
    @Delete
    suspend fun deleteCard(card: Card)
}
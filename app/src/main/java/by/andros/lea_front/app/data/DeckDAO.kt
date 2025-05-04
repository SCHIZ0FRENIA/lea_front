package by.andros.lea_front.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY created_at DESC")
    fun getAllDecks(): Flow<List<Deck>>
    
    @Query("SELECT * FROM decks WHERE id = :deckId")
    fun getDeckById(deckId: Long): Flow<Deck>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck): Long
    
    @Update
    suspend fun updateDeck(deck: Deck)
    
    @Delete
    suspend fun deleteDeck(deck: Deck)
}
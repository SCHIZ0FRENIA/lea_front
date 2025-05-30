package by.andros.lea_front.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {
    @Insert
    suspend fun insertSession(session: LearningSession): Long

    @Query("UPDATE learning_sessions SET endTime = :endTime WHERE id = :sessionId")
    suspend fun updateSessionEndTime(sessionId: Long, endTime: String)

    @Insert
    suspend fun insertCardAnswer(cardAnswer: CardAnswer): Long

    @Query("SELECT COUNT(*) FROM learning_sessions")
    fun getTotalSessionsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM card_answers")
    fun getTotalAnswersCount(): Flow<Int>

    @Query("SELECT rating, COUNT(*) as count FROM card_answers GROUP BY rating ORDER BY rating")
    fun getAnswerCountsByRating(): Flow<List<AnswerCount>>

    @Transaction
    @Query("SELECT * FROM learning_sessions ORDER BY startTime DESC LIMIT 10")
    fun getRecentSessions(): Flow<List<LearningSession>>
    
    @Query("SELECT COUNT(*) FROM card_answers WHERE rating = :rating")
    fun getAnswerCountForRating(rating: Int): Flow<Int>
} 
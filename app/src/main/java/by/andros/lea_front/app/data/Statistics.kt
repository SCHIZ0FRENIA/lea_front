package by.andros.lea_front.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "learning_sessions")
data class LearningSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: String,
    val endTime: String?,
    val cardCount: Int
)

@Entity(tableName = "card_answers")
data class CardAnswer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val cardId: Long,
    val rating: Int, // 1-5 rating
    val timestamp: String
)

// For aggregated statistics
data class AnswerCount(
    val rating: Int,
    val count: Int
)

data class SessionStats(
    val totalSessions: Int,
    val totalCards: Int,
    val answerCounts: List<AnswerCount>
) 
package by.andros.lea_front.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepository @Inject constructor(
    private val statisticsDao: StatisticsDao
) {
    private var currentSessionId: Long? = null
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    suspend fun startLearningSession(cardCount: Int): Long {
        val session = LearningSession(
            startTime = LocalDateTime.now().format(dateTimeFormatter),
            endTime = null,
            cardCount = cardCount
        )
        val sessionId = statisticsDao.insertSession(session)
        currentSessionId = sessionId
        return sessionId
    }

    suspend fun endLearningSession() {
        currentSessionId?.let { sessionId ->
            statisticsDao.updateSessionEndTime(
                sessionId = sessionId,
                endTime = LocalDateTime.now().format(dateTimeFormatter)
            )
            currentSessionId = null
        }
    }

    suspend fun recordCardAnswer(cardId: Long, rating: Int) {
        currentSessionId?.let { sessionId ->
            val cardAnswer = CardAnswer(
                sessionId = sessionId,
                cardId = cardId,
                rating = rating,
                timestamp = LocalDateTime.now().format(dateTimeFormatter)
            )
            statisticsDao.insertCardAnswer(cardAnswer)
        }
    }

    fun getSessionStats(): Flow<SessionStats> {
        return combine(
            statisticsDao.getTotalSessionsCount(),
            statisticsDao.getTotalAnswersCount(),
            statisticsDao.getAnswerCountsByRating()
        ) { sessionCount, answerCount, answerCounts ->
            // Ensure we have entries for all possible ratings (1-5)
            val completeAnswerCounts = List(5) { rating ->
                val existingCount = answerCounts.find { it.rating == rating + 1 }
                if (existingCount != null) {
                    existingCount
                } else {
                    AnswerCount(rating = rating + 1, count = 0)
                }
            }
            
            SessionStats(
                totalSessions = sessionCount,
                totalCards = answerCount,
                answerCounts = completeAnswerCounts
            )
        }
    }

    fun getAnswerCountForRating(rating: Int): Flow<Int> {
        return statisticsDao.getAnswerCountForRating(rating)
    }
} 
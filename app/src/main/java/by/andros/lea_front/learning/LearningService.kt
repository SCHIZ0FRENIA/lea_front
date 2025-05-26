package by.andros.lea_front.learning

import by.andros.lea_front.app.data.Card
import kotlin.math.roundToInt
import kotlin.random.Random

// Data class to hold the state of a learning session internally or for pausing
data class InternalLearningSessionState(
    val originalCards: List<Card>, // All cards for this session, in their initial shuffled order
    val reviewQueue: MutableList<Card>, // Cards remaining to be reviewed in this pass
    val currentCard: Card?,
    val currentIndexInQueue: Int, // Index in the reviewQueue
    val showAnswer: Boolean,
    val ratingCounts: MutableMap<Int, Int>,
    val sessionTotalCards: Int
)

// Statistics to be shown at the end of a session
data class LearningSessionStats(
    val ratingCounts: Map<Int, Int>, // Map of rating (1-5) to count
    val totalCardsReviewed: Int
)

class LearningService {
    private var currentSessionState: InternalLearningSessionState? = null

    fun startSession(cardsForSession: List<Card>) {
        if (cardsForSession.isEmpty()) {
            currentSessionState = null
            return
        }
        val shuffledCards = cardsForSession.shuffled(Random(System.currentTimeMillis()))
        currentSessionState = InternalLearningSessionState(
            originalCards = shuffledCards,
            reviewQueue = shuffledCards.toMutableList(),
            currentCard = shuffledCards.first(),
            currentIndexInQueue = 0,
            showAnswer = false,
            ratingCounts = mutableMapOf(),
            sessionTotalCards = shuffledCards.size
        )
    }

    fun getCurrentCard(): Card? = currentSessionState?.currentCard

    fun getCurrentProgress(): Pair<Int, Int>? { // Pair of (current card number, total cards)
        currentSessionState?.let {
            // Current card number is (total cards - remaining in queue + 1) if a card is active
            // or based on currentIndexInQueue
            val reviewedCount = it.originalCards.size - it.reviewQueue.size
            val currentNumber = if (it.currentCard != null) reviewedCount + 1 else reviewedCount
            return Pair(currentNumber.coerceAtMost(it.sessionTotalCards), it.sessionTotalCards)
        }
        return null
    }


    fun showAnswer() {
        currentSessionState = currentSessionState?.copy(showAnswer = true)
    }

    fun submitAnswer(rating: Int): Card? { // Returns the next card or null if session finished
        currentSessionState?.let { state ->
            val cardRated = state.currentCard ?: return null // The card that was just rated

            // Always remove the card from its current position in the queue.
            // If it's re-queued, it will be added to the end.
            state.reviewQueue.remove(cardRated)

            if (rating <= 2) {
                // Low rating: Re-queue the card at the end.
                // Do NOT record this low rating in the statistics for this attempt.
                state.reviewQueue.add(cardRated)
            } else {
                // Good rating (3, 4, or 5): Card is considered passed for this round.
                // Record this rating for the session statistics.
                state.ratingCounts[rating] = (state.ratingCounts[rating] ?: 0) + 1
            }

            // Determine the next card from the front of the possibly modified reviewQueue.
            val nextCard = state.reviewQueue.firstOrNull()

            currentSessionState = state.copy(
                currentCard = nextCard,
                showAnswer = false
                // currentIndexInQueue might not be as relevant if the queue is dynamic.
                // The primary logic is to always take from the head of reviewQueue.
            )
            return nextCard
        }
        return null
    }

    fun isSessionFinished(): Boolean = currentSessionState?.reviewQueue?.isEmpty() ?: true

    fun getStats(): LearningSessionStats? {
        currentSessionState?.let {
            return LearningSessionStats(
                ratingCounts = it.ratingCounts.toMap(),
                totalCardsReviewed = it.sessionTotalCards
            )
        }
        return null
    }

    fun pauseSession(): InternalLearningSessionState? {
        // Current state is already the savable state
        return currentSessionState
    }

    fun resumeSession(pausedState: InternalLearningSessionState) {
        currentSessionState = pausedState
    }

    fun endSession() {
        currentSessionState = null
    }
} 
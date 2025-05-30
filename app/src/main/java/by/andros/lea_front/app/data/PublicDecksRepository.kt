package by.andros.lea_front.app.data

import android.util.Log
import by.andros.lea_front.app.data.api.PublicDecksApiService
import by.andros.lea_front.app.data.model.PublicDeckResponse
import by.andros.lea_front.auth.domain.AuthRepository
import by.andros.lea_front.app.data.Deck
import by.andros.lea_front.app.data.Card
import by.andros.lea_front.app.data.CardRepository
import by.andros.lea_front.app.data.DeckRepository
import by.andros.lea_front.app.data.api.PublicDeckRequest
import by.andros.lea_front.app.data.api.PublicCardRequest
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first

@Singleton
class PublicDecksRepository @Inject constructor(
    private val publicDecksApiService: PublicDecksApiService,
    private val authRepository: AuthRepository,
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository
) {
    // Helper method to check for and handle token expiration
    private suspend fun handleAuthError(errorBody: String?, errorMessage: String): Exception {
        // If the error indicates expired token, clear auth data to force re-login
        if (errorBody?.contains("Signature has expired") == true || 
            errorMessage.contains("expired", ignoreCase = true) ||
            errorMessage.contains("Authentication required", ignoreCase = true)) {
            
            Log.w("AUTH_ERROR", "JWT token expired, clearing auth data")
            authRepository.clearAuthData()
            return Exception("Your session has expired. Please log in again.")
        }
        return Exception(errorMessage)
    }

    fun getAllPublicDecks(skip: Int = 0, limit: Int = 20): Flow<Result<List<PublicDeckResponse>>> = flow {
        try {
            val response = publicDecksApiService.getAllPublicDecks(skip, limit)
            if (response.isSuccessful) {
                emit(Result.success(response.body() ?: emptyList()))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val json = JsonParser.parseString(errorBody).asJsonObject
                    json["message"]?.asString ?: "Failed to fetch public decks"
                } catch (e: Exception) {
                    errorBody ?: "Failed to fetch public decks"
                }
                emit(Result.failure(handleAuthError(errorBody, errorMessage)))
            }
        } catch (e: Exception) {
            Log.e("PUBLIC_DECKS", "Error fetching public decks: ${e.message}", e)
            emit(Result.failure(e))
        }
    }
    
    fun searchPublicDecks(query: String, skip: Int = 0, limit: Int = 20): Flow<Result<List<PublicDeckResponse>>> = flow {
        try {
            val response = publicDecksApiService.searchPublicDecks(query, skip, limit)
            if (response.isSuccessful) {
                emit(Result.success(response.body() ?: emptyList()))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val json = JsonParser.parseString(errorBody).asJsonObject
                    json["message"]?.asString ?: "Failed to search public decks"
                } catch (e: Exception) {
                    errorBody ?: "Failed to search public decks"
                }
                emit(Result.failure(handleAuthError(errorBody, errorMessage)))
            }
        } catch (e: Exception) {
            Log.e("PUBLIC_DECKS", "Error searching public decks: ${e.message}", e)
            emit(Result.failure(e))
        }
    }
    
    fun getPublicDeck(deckId: String): Flow<Result<PublicDeckResponse>> = flow {
        try {
            val response = publicDecksApiService.getPublicDeck(deckId)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(Result.success(it))
                } ?: emit(Result.failure(Exception("Deck not found")))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val json = JsonParser.parseString(errorBody).asJsonObject
                    json["message"]?.asString ?: "Failed to fetch public deck"
                } catch (e: Exception) {
                    errorBody ?: "Failed to fetch public deck"
                }
                emit(Result.failure(handleAuthError(errorBody, errorMessage)))
            }
        } catch (e: Exception) {
            Log.e("PUBLIC_DECKS", "Error fetching public deck: ${e.message}", e)
            emit(Result.failure(e))
        }
    }
    
    suspend fun publishDeck(deck: Deck): Result<String> {
        try {
            // Clear, extensive logging
            Log.d("PublicDecksRepository", "==== PUBLISHING DECK ====")
            Log.d("PublicDecksRepository", "Deck ID: ${deck.id}")
            Log.d("PublicDecksRepository", "Deck name: ${deck.name}")
            Log.d("PublicDecksRepository", "Deck description: ${deck.description}")
            Log.d("PublicDecksRepository", "Created by: ${deck.createdBy}")
            
            // Check auth token
            val token = authRepository.getJwtToken() 
            if (token == null) {
                Log.e("PublicDecksRepository", "ERROR: JWT token is null - not authenticated")
                return Result.failure(Exception("Authentication required - please log in first"))
            }
            Log.d("PublicDecksRepository", "JWT token is valid")
            
            // Get cards for this deck from local database
            Log.d("PublicDecksRepository", "Fetching cards for deck ID: ${deck.id}")
            val deckCards = deck.id?.let { deckId ->
                try {
                    val cards = cardRepository.getCardsByDeck(deckId).first()
                    Log.d("PublicDecksRepository", "Successfully fetched ${cards.size} cards")
                    cards
                } catch (e: Exception) {
                    Log.e("PublicDecksRepository", "Error fetching cards: ${e.message}", e)
                    emptyList()
                }
            } ?: run {
                Log.e("PublicDecksRepository", "Deck ID is null, cannot fetch cards")
                emptyList()
            }
            
            // Validate if deck has cards
            if (deckCards.isEmpty()) {
                Log.e("PublicDecksRepository", "Cannot publish an empty deck")
                return Result.failure(Exception("Cannot publish an empty deck. Please add cards first."))
            }
            
            Log.d("PublicDecksRepository", "Cards for deck:")
            deckCards.forEachIndexed { index, card ->
                Log.d("PublicDecksRepository", "Card $index: front='${card.front}', back='${card.back}'")
            }
            
            // Convert cards to the format expected by the backend
            val formattedCards = deckCards.map { card ->
                PublicCardRequest(
                    id = card.id?.toString() ?: "0",
                    front = card.front,
                    back = card.back
                )
            }
            
            // Convert local deck to publishable format exactly matching backend schema
            val deckRequest = PublicDeckRequest(
                name = deck.name,
                description = deck.description ?: "",
                cards = formattedCards
            )
            
            Log.d("PublicDecksRepository", "Final request data: $deckRequest")
            
            // Make the API call
            Log.d("PublicDecksRepository", "Making API call to publish deck")
            Log.d("PublicDecksRepository", "API URL: v1/decks")
            Log.d("PublicDecksRepository", "Method: POST")
            
            try {
                val response = publicDecksApiService.createPublicDeck("Bearer $token", deckRequest)
                Log.d("PublicDecksRepository", "API response received - Status code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d("PublicDecksRepository", "Success response: $responseBody")
                    return Result.success("Deck published successfully")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("PublicDecksRepository", "Error response: $errorBody")
                    Log.e("PublicDecksRepository", "Response code: ${response.code()}")
                    
                    val errorMessage = try {
                        val json = JsonParser.parseString(errorBody).asJsonObject
                        val message = json["message"]?.asString ?: "Failed to publish deck"
                        message
                    } catch (e: Exception) {
                        errorBody ?: "Failed to publish deck"
                    }
                    
                    return Result.failure(handleAuthError(errorBody, errorMessage))
                }
            } catch (apiException: Exception) {
                Log.e("PublicDecksRepository", "API call failed: ${apiException.message}", apiException)
                return Result.failure(Exception("Network error publishing deck: ${apiException.message}"))
            }
        } catch (e: Exception) {
            Log.e("PublicDecksRepository", "Unexpected error publishing deck: ${e.message}", e)
            return Result.failure(Exception("Unexpected error publishing deck: ${e.message}"))
        }
    }
    
    suspend fun downloadPublicDeck(deckId: String): Result<String> {
        try {
            Log.d("PublicDecksRepository", "==== DOWNLOADING PUBLIC DECK ====")
            Log.d("PublicDecksRepository", "Deck ID: $deckId")
            
            val token = authRepository.getJwtToken() ?: return Result.failure(Exception("Authentication required"))
            
            // Step 1: First get the public deck details to have them available
            val publicDeckResponse = publicDecksApiService.getPublicDeck(deckId)
            if (!publicDeckResponse.isSuccessful) {
                val errorBody = publicDeckResponse.errorBody()?.string()
                Log.e("PublicDecksRepository", "Error fetching public deck: $errorBody")
                return Result.failure(Exception("Failed to get public deck details: ${publicDeckResponse.code()}"))
            }
            
            val publicDeck = publicDeckResponse.body() ?: return Result.failure(Exception("Public deck not found"))
            Log.d("PublicDecksRepository", "Fetched public deck: ${publicDeck.name} with ${publicDeck.cards.size} cards")
            
            // Step 2: Download the deck (add it to user's collection on server)
            val response = publicDecksApiService.downloadPublicDeck("Bearer $token", deckId)
            
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val json = JsonParser.parseString(errorBody).asJsonObject
                    json["message"]?.asString ?: "Failed to download deck"
                } catch (e: Exception) {
                    errorBody ?: "Failed to download deck"
                }
                return Result.failure(handleAuthError(errorBody, errorMessage))
            }
            
            // Step 3: Convert the public deck to a local deck and save it
            try {
                // Create a new Deck object for local storage
                val localDeck = Deck(
                    name = publicDeck.name,
                    description = publicDeck.description,
                    createdBy = authRepository.getUserLogin() ?: "current_user", // Get the current user's login
                    source = "public" // Mark as coming from public deck
                )
                
                // Insert the deck to get its ID
                Log.d("PublicDecksRepository", "Inserting deck into local database: ${localDeck.name}")
                val deckId = try {
                    deckRepository.insertDeck(localDeck)
                } catch (e: Exception) {
                    Log.e("PublicDecksRepository", "Error inserting deck: ${e.message}", e)
                    return Result.failure(Exception("Failed to save deck locally: ${e.message}"))
                }
                
                Log.d("PublicDecksRepository", "Successfully inserted deck with ID: $deckId")
                
                // Create and insert all cards
                var cardsInserted = 0
                try {
                    publicDeck.cards.forEach { publicCard ->
                        val currentTime = System.currentTimeMillis() / 1000 // Current time in seconds
                        val card = Card(
                            deckId = deckId,
                            front = publicCard.front,
                            back = publicCard.back,
                            lastReview = currentTime.toInt(),
                            nextReview = currentTime.toInt() // Set next review to current time so it's available for review
                        )
                        cardRepository.insertCard(card)
                        cardsInserted++
                    }
                    Log.d("PublicDecksRepository", "Successfully inserted $cardsInserted cards")
                } catch (e: Exception) {
                    Log.e("PublicDecksRepository", "Error inserting cards: ${e.message}", e)
                    // Continue even if some cards fail to insert
                }
                
                Log.d("PublicDecksRepository", "Successfully saved deck locally with ID: $deckId")
            } catch (e: Exception) {
                Log.e("PublicDecksRepository", "Error saving deck locally: ${e.message}", e)
                // We still return success since the server copy was created successfully
            }
            
            return Result.success("Deck downloaded successfully")
        } catch (e: Exception) {
            Log.e("PUBLIC_DECKS", "Error downloading deck: ${e.message}", e)
            return Result.failure(e)
        }
    }
    
    suspend fun deletePublicDeck(deckId: String): Result<String> {
        try {
            val token = authRepository.getJwtToken() ?: return Result.failure(Exception("Authentication required"))
            
            val response = publicDecksApiService.deletePublicDeck("Bearer $token", deckId)
            
            return if (response.isSuccessful) {
                Result.success("Deck deleted successfully")
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val json = JsonParser.parseString(errorBody).asJsonObject
                    json["message"]?.asString ?: "Failed to delete deck"
                } catch (e: Exception) {
                    errorBody ?: "Failed to delete deck"
                }
                Result.failure(handleAuthError(errorBody, errorMessage))
            }
        } catch (e: Exception) {
            Log.e("PUBLIC_DECKS", "Error deleting deck: ${e.message}", e)
            return Result.failure(e)
        }
    }
} 
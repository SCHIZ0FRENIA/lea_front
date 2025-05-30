package by.andros.lea_front.app.data.api

import by.andros.lea_front.app.data.model.PublicDeckResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PublicDecksApiService {
    @GET("v1/decks")
    suspend fun getAllPublicDecks(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20
    ): Response<List<PublicDeckResponse>>
    
    @GET("v1/decks/search")
    suspend fun searchPublicDecks(
        @Query("q") query: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20
    ): Response<List<PublicDeckResponse>>
    
    @GET("v1/decks/{deckId}")
    suspend fun getPublicDeck(
        @Path("deckId") deckId: String
    ): Response<PublicDeckResponse>
    
    @POST("v1/decks/")
    suspend fun createPublicDeck(
        @Header("Authorization") token: String,
        @Body deck: PublicDeckRequest
    ): Response<Map<String, String>>
    
    @DELETE("v1/decks/{deckId}")
    suspend fun deletePublicDeck(
        @Header("Authorization") token: String,
        @Path("deckId") deckId: String
    ): Response<Map<String, String>>
    
    @POST("v1/decks/{deckId}/download")
    suspend fun downloadPublicDeck(
        @Header("Authorization") token: String,
        @Path("deckId") deckId: String
    ): Response<Map<String, String>>
}

data class PublicDeckRequest(
    val name: String,
    val description: String,
    val cards: List<PublicCardRequest>
)

data class PublicCardRequest(
    val id: String,
    val front: String,
    val back: String
) 
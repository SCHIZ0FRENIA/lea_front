package by.andros.lea_front.app.data.model

import com.google.gson.annotations.SerializedName

data class PublicDeckResponse(
    @SerializedName("_id")
    val id: String,
    
    val name: String,
    
    val description: String?,
    
    @SerializedName("created_by")
    val createdBy: String,
    
    val cards: List<PublicCardResponse> = emptyList()
)

data class PublicCardResponse(
    val id: String,
    val front: String,
    val back: String
) 
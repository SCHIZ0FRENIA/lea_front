package by.andros.lea_front.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        )
    ]
)
class Card (
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    
    val front: String,
    val back: String,
    val repetitions: Int = 0,
    val interval: Int = 1,
    
    @ColumnInfo(name = "last_review")
    val lastReview: Int,
    
    @ColumnInfo(name = "next_review")
    val nextReview: Int,
    
    @ColumnInfo(name = "ease_factor")
    val easeFactor: Float = 2.5f,
    
    @ColumnInfo(name = "deck_id")
    val deckId: Long,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
package by.andros.lea_front.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    
    val name: String,
    val description: String?,
    
    @ColumnInfo(name = "created_by")
    val createdBy: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    val source: String = "user"
)
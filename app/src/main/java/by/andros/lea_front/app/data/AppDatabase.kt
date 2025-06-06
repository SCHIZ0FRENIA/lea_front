package by.andros.lea_front.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import javax.inject.Singleton

@Database(
    entities = [Deck::class, Card::class, LearningSession::class, CardAnswer::class],
    version = 3,
    exportSchema = false
)
@Singleton
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun statisticsDao(): StatisticsDao
    
    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "lea_database"
            ).fallbackToDestructiveMigration()
                .build()
        }
    }
}
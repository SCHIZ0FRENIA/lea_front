package by.andros.lea_front.di

import android.content.Context
import by.andros.lea_front.app.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.create(context)
    }
    
    @Provides
    fun provideDeckDao(database: AppDatabase) = database.deckDao()
    
    @Provides
    fun provideCardDao(database: AppDatabase) = database.cardDao()
}
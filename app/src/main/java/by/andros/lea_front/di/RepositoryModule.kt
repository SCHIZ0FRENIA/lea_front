package by.andros.lea_front.di

import by.andros.lea_front.auth.domain.AuthRepository
import by.andros.lea_front.auth.domain.AuthRepositoryImpl
import by.andros.lea_front.app.data.PublicDecksRepository
import by.andros.lea_front.app.data.CardRepository
import by.andros.lea_front.app.data.DeckRepository
import by.andros.lea_front.app.data.api.PublicDecksApiService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    companion object {
        @Provides
        @Singleton
        fun providePublicDecksRepository(
            publicDecksApiService: PublicDecksApiService,
            authRepository: AuthRepository,
            cardRepository: CardRepository,
            deckRepository: DeckRepository
        ): PublicDecksRepository {
            return PublicDecksRepository(publicDecksApiService, authRepository, cardRepository, deckRepository)
        }
    }
}
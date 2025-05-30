package by.andros.lea_front.di

import by.andros.lea_front.auth.LoginConstants
import by.andros.lea_front.auth.data.AuthApiService
import by.andros.lea_front.app.data.api.PublicDecksApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        // Simplified version without the logging interceptor
        return OkHttpClient.Builder().build()
    }
    
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(LoginConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }
    
    @Provides
    fun providePublicDecksApiService(retrofit: Retrofit): PublicDecksApiService {
        return retrofit.create(PublicDecksApiService::class.java)
    }
}
/* Dependency-injection module configuring Retrofit, OkHttp, logging, and dynamic Firefly routing. */
package com.teja.finfly.di

import com.teja.finfly.BuildConfig
import com.teja.finfly.data.network.FireflyApiService
import com.teja.finfly.data.network.FireflyAuthInterceptor
import com.teja.finfly.data.network.FireflyConnectionTester
import com.teja.finfly.data.network.ServerUrlInterceptor
import com.teja.finfly.domain.repository.ConnectionTester
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingsModule {
    @Binds
    @Singleton
    abstract fun bindConnectionTester(implementation: FireflyConnectionTester): ConnectionTester
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
    }

    @Provides
    @Singleton
    @Named("bare")
    fun provideBareClient(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(logging).build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: FireflyAuthInterceptor,
        serverUrlInterceptor: ServerUrlInterceptor,
        logging: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(serverUrlInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.DEFAULT_SERVER_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideFireflyApi(retrofit: Retrofit): FireflyApiService =
        retrofit.create(FireflyApiService::class.java)
}

/* Dependency-injection bindings from domain repositories to data implementations. */
package com.teja.finfly.di

import com.teja.finfly.data.repository.AccountRepositoryImpl
import com.teja.finfly.data.repository.TransactionRepositoryImpl
import com.teja.finfly.data.repository.FireflyFeatureRepositoryImpl
import com.teja.finfly.domain.repository.AccountRepository
import com.teja.finfly.domain.repository.TransactionRepository
import com.teja.finfly.domain.repository.FireflyFeatureRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(implementation: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(implementation: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindFireflyFeatureRepository(
        implementation: FireflyFeatureRepositoryImpl,
    ): FireflyFeatureRepository
}

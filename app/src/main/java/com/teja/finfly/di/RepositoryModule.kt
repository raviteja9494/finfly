/* Dependency-injection bindings from domain repositories to data implementations. */
package com.teja.finfly.di

import com.teja.finfly.data.repository.AccountRepositoryImpl
import com.teja.finfly.data.repository.TransactionRepositoryImpl
import com.teja.finfly.data.repository.FireflyFeatureRepositoryImpl
import com.teja.finfly.data.repository.TagRepositoryImpl
import com.teja.finfly.domain.repository.AccountRepository
import com.teja.finfly.domain.repository.TransactionRepository
import com.teja.finfly.domain.repository.FireflyFeatureRepository
import com.teja.finfly.domain.repository.TagRepository
import com.teja.finfly.data.repository.SmsRulesRepositoryImpl
import com.teja.finfly.data.repository.SmsLogRepositoryImpl
import com.teja.finfly.data.repository.RulesTransferRepositoryImpl
import com.teja.finfly.data.sms.RuleBasedSmsParserFactory
import com.teja.finfly.domain.repository.SmsRulesRepository
import com.teja.finfly.domain.repository.SmsLogRepository
import com.teja.finfly.domain.repository.RulesTransferRepository
import com.teja.finfly.domain.sms.SmsParserFactory
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

    @Binds
    @Singleton
    abstract fun bindTagRepository(implementation: TagRepositoryImpl): TagRepository

    @Binds
    @Singleton
    abstract fun bindSmsRulesRepository(implementation: SmsRulesRepositoryImpl): SmsRulesRepository

    @Binds
    @Singleton
    abstract fun bindSmsLogRepository(implementation: SmsLogRepositoryImpl): SmsLogRepository

    @Binds
    @Singleton
    abstract fun bindRulesTransferRepository(
        implementation: RulesTransferRepositoryImpl,
    ): RulesTransferRepository

    @Binds
    @Singleton
    abstract fun bindSmsParserFactory(implementation: RuleBasedSmsParserFactory): SmsParserFactory
}

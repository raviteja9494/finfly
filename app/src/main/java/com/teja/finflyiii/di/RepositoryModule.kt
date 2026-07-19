/* Dependency-injection bindings from domain repositories to data implementations. */
package com.teja.finflyiii.di

import com.teja.finflyiii.data.repository.AccountRepositoryImpl
import com.teja.finflyiii.data.repository.TransactionRepositoryImpl
import com.teja.finflyiii.data.repository.FireflyFeatureRepositoryImpl
import com.teja.finflyiii.data.repository.TagRepositoryImpl
import com.teja.finflyiii.domain.repository.AccountRepository
import com.teja.finflyiii.domain.repository.TransactionRepository
import com.teja.finflyiii.domain.repository.FireflyFeatureRepository
import com.teja.finflyiii.domain.repository.TagRepository
import com.teja.finflyiii.domain.repository.SmsInboxRepository
import com.teja.finflyiii.data.sms.AndroidSmsInboxRepository
import com.teja.finflyiii.data.repository.SmsRulesRepositoryImpl
import com.teja.finflyiii.data.repository.SmsLogRepositoryImpl
import com.teja.finflyiii.data.repository.RulesTransferRepositoryImpl
import com.teja.finflyiii.data.sms.RuleBasedSmsParserFactory
import com.teja.finflyiii.domain.repository.SmsRulesRepository
import com.teja.finflyiii.domain.repository.SmsLogRepository
import com.teja.finflyiii.domain.repository.RulesTransferRepository
import com.teja.finflyiii.domain.sms.SmsParserFactory
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
    abstract fun bindSmsInboxRepository(implementation: AndroidSmsInboxRepository): SmsInboxRepository
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

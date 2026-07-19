/* Dependency-injection bindings for the swappable local assistant provider. */
package com.teja.finflyiii.di

import com.teja.finflyiii.data.ai.AiRepositoryImpl
import com.teja.finflyiii.data.ai.LiteRtFinanceAssistant
import com.teja.finflyiii.domain.assistant.FinanceAssistant
import com.teja.finflyiii.domain.repository.AiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds
    @Singleton
    abstract fun bindAiRepository(implementation: AiRepositoryImpl): AiRepository

    @Binds
    @Singleton
    abstract fun bindFinanceAssistant(implementation: LiteRtFinanceAssistant): FinanceAssistant
}

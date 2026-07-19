/* Dependency-injection bindings for the swappable local assistant provider. */
package com.teja.finfly.di

import com.teja.finfly.data.ai.AiRepositoryImpl
import com.teja.finfly.data.ai.MediaPipeFinanceAssistant
import com.teja.finfly.domain.assistant.FinanceAssistant
import com.teja.finfly.domain.repository.AiRepository
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
    abstract fun bindFinanceAssistant(implementation: MediaPipeFinanceAssistant): FinanceAssistant
}

/* Application entry point that initializes FinFly III's Hilt dependency graph. */
package com.teja.finflyiii

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.teja.finflyiii.di.ApplicationScope
import com.teja.finflyiii.domain.repository.SmsRulesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FinFlyIIIApplication : Application() {
    @Inject lateinit var smsRulesRepository: SmsRulesRepository
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { smsRulesRepository.ensureDefaults() }
    }
}

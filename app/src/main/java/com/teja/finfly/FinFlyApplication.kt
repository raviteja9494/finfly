/* Application entry point that initializes FinFly's Hilt dependency graph. */
package com.teja.finfly

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.teja.finfly.di.ApplicationScope
import com.teja.finfly.domain.repository.SmsRulesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FinFlyApplication : Application() {
    @Inject lateinit var smsRulesRepository: SmsRulesRepository
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { smsRulesRepository.ensureDefaults() }
    }
}

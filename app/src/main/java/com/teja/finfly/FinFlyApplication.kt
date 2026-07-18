/* Application entry point that initializes FinFly's Hilt dependency graph. */
package com.teja.finfly

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FinFlyApplication : Application()

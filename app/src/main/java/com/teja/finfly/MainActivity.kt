/* Android activity hosting the single-activity Jetpack Compose application. */
package com.teja.finfly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.teja.finfly.presentation.navigation.FinFlyApp
import com.teja.finfly.presentation.theme.FinFlyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinFlyTheme { FinFlyApp() }
        }
    }
}

/* Android activity hosting the single-activity Jetpack Compose application. */
package com.teja.finflyiii

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.teja.finflyiii.presentation.navigation.FinFlyIIIApp
import com.teja.finflyiii.presentation.theme.FinFlyIIITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinFlyIIITheme { FinFlyIIIApp() }
        }
    }
}

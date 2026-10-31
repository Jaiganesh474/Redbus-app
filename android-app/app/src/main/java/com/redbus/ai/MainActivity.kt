package com.redbus.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.redbus.ai.data.local.SessionManager
import com.redbus.ai.ui.navigation.RedbusAiApp
import com.redbus.ai.ui.screens.RedbusSplashScreen
import com.redbus.ai.ui.viewmodel.RedbusViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        SessionManager.init(applicationContext)
        val viewModel = ViewModelProvider(this)[RedbusViewModel::class.java]
        setContent {
            var showSplash by remember { mutableStateOf(true) }

            Crossfade(
                targetState = showSplash,
                animationSpec = tween(durationMillis = 500),
                label = "splashCrossfade"
            ) { isSplash ->
                if (isSplash) {
                    RedbusSplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    RedbusAiApp(viewModel = viewModel)
                }
            }
        }
    }
}

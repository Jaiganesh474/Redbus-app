package com.redbus.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.redbus.ai.ui.navigation.RedbusAiApp
import com.redbus.ai.ui.viewmodel.RedbusViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RedbusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RedbusAiApp(viewModel = viewModel)
        }
    }
}


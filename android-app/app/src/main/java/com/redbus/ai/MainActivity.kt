package com.redbus.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.redbus.ai.data.local.SessionManager
import com.redbus.ai.ui.navigation.RedbusAiApp
import com.redbus.ai.ui.viewmodel.RedbusViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(applicationContext)
        val viewModel = ViewModelProvider(this)[RedbusViewModel::class.java]
        setContent {
            RedbusAiApp(viewModel = viewModel)
        }
    }
}

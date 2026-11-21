package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AgentViewModel
import com.example.ui.FullstackAgentApp
import com.example.ui.theme.FullstackAgentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AgentViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            FullstackAgentTheme(darkTheme = state.config.darkTheme) {
                FullstackAgentApp(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }
}

package com.urg.edge

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urg.edge.ui.ChatScreen

@Composable
fun App(
    viewModel: ChatViewModel,
    onMicStart: () -> Unit,
    onMicStop: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MaterialTheme {
        ChatScreen(
            uiState = uiState,
            onPromptChange = viewModel::updatePrompt,
            onSendClick = viewModel::onSendClick,
            onTriageClick = viewModel::startTriage,
            onMicStart = onMicStart,
            onMicStop = onMicStop,
        )
    }
}

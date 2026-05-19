package com.urg.edge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.urg.edge.ChatUiState
import com.urg.edge.Strings

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onTriageClick: () -> Unit,
    onMicStart: () -> Unit,
    onMicStop: () -> Unit,
    onTestWavRecognize: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = Strings.APP_TITLE)
            OutlinedButton(onClick = onTestWavRecognize) {
                Text(Strings.BUTTON_TEST_WAV)
            }
        }

        MessageList(
            messages = uiState.messages,
            streamingText = uiState.streamingText,
            modifier = Modifier.weight(1f)
        )

        if (uiState.isLoading && uiState.streamingText.isEmpty()) {
            Text(Strings.LOADING_LABEL)
        }
        if (uiState.isListening) {
            Text(Strings.LISTENING_LABEL)
        }

        PromptInput(value = uiState.promptText, onValueChange = onPromptChange)
        ActionButtons(
            isListening = uiState.isListening,
            onTriageClick = onTriageClick,
            onMicStart = onMicStart,
            onMicStop = onMicStop,
            onSendClick = onSendClick,
        )
    }
}

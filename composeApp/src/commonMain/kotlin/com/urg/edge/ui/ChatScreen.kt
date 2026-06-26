package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urg.edge.ChatUiState
import com.urg.edge.Strings

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onVoiceInputClick: () -> Unit,
    onMicStart: () -> Unit,
    onMicStop: () -> Unit,
    onTestWavRecognize: (String) -> Unit,
    onTriageYes: () -> Unit = {},
    onTriageNo: () -> Unit = {},
    applyStatusBarPadding: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F6))
            .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
    ) {
        MessageList(
            messages = uiState.messages,
            streamingText = uiState.streamingText,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (uiState.showTriageButtons) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFE8FAF3))
                            .clickable { onTriageYes() }
                    ) {
                        Text(
                            text = "✓  ${Strings.BUTTON_YES}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E9E6E),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFFFF0F0))
                            .clickable { onTriageNo() }
                    ) {
                        Text(
                            text = "✕  ${Strings.BUTTON_NO}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD94444),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            PromptInput(
                value = uiState.promptText,
                onValueChange = onPromptChange,
                onSendClick = onSendClick,
            )

            Spacer(modifier = Modifier.height(10.dp))

            ActionButtons(
                isListening = uiState.isListening,
                onVoiceInputClick = onVoiceInputClick,
            )
        }
    }
}

package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
fun TriageTabScreen(
    showChat: Boolean,
    onToggle: (Boolean) -> Unit,
    uiState: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onVoiceInputClick: () -> Unit,
    onMicStart: () -> Unit,
    onMicStop: () -> Unit,
    onTestWavRecognize: (String) -> Unit,
    onTriageYes: () -> Unit,
    onTriageNo: () -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F6))
    ) {
        // ── トグルヘッダー ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(10.dp))
                    .padding(3.dp)
            ) {
                // トリアージ タブ
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!showChat) Color.White else Color.Transparent)
                        .clickable { onToggle(false) }
                        .padding(vertical = 7.dp)
                ) {
                    Text(
                        text = "トリアージ",
                        fontSize = 13.sp,
                        fontWeight = if (!showChat) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (!showChat) Color(0xFF25B1BF) else Color(0xFF888888),
                    )
                }
                // チャット タブ
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showChat) Color.White else Color.Transparent)
                        .clickable { onToggle(true) }
                        .padding(vertical = 7.dp)
                ) {
                    Text(
                        text = "チャット",
                        fontSize = 13.sp,
                        fontWeight = if (showChat) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (showChat) Color(0xFF25B1BF) else Color(0xFF888888),
                    )
                }
            }
        }

        // ── コンテンツ ───────────────────────────────────────────────────
        if (showChat) {
            ChatScreen(
                uiState = uiState,
                onPromptChange = onPromptChange,
                onSendClick = onSendClick,
                onVoiceInputClick = onVoiceInputClick,
                onMicStart = onMicStart,
                onMicStop = onMicStop,
                onTestWavRecognize = onTestWavRecognize,
                onTriageYes = onTriageYes,
                onTriageNo = onTriageNo,
                applyStatusBarPadding = false,
                modifier = Modifier.weight(1f),
            )
        } else {
            TriageFlowScreen(
                showHeader = false,
                onNavigateToMap = onNavigateToMap,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

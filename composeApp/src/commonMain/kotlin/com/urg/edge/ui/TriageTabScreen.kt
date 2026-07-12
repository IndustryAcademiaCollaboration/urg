package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.zIndex
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
import com.urg.edge.ChatScope
import com.urg.edge.ChatUiState
import com.urg.edge.ChatViewModel
import com.urg.edge.VictimRecord

@Composable
fun TriageTabScreen(
    chatViewModel: ChatViewModel,
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
    victims: List<VictimRecord> = emptyList(),
    onScopeChange: (ChatScope) -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F6))
            .padding(bottom = bottomPadding)
    ) {
        // ── タブ切替 ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF1F4F7))
                    .padding(4.dp)
            ) {
                // トリアージ タブ
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(50))
                        .background(if (!showChat) Color.White else Color.Transparent)
                        .clickable { onToggle(false) }
                ) {
                    Text(
                        text = "トリアージ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!showChat) Color(0xFF1D828E) else Color(0xFF9AACB4),
                    )
                }
                // チャット タブ
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(50))
                        .background(if (showChat) Color.White else Color.Transparent)
                        .clickable { onToggle(true) }
                ) {
                    Text(
                        text = "チャット",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showChat) Color(0xFF1D828E) else Color(0xFF9AACB4),
                    )
                }
            }
        }

        // ── コンテンツ（両方常に描画して zIndex で前面切り替え）──────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF2F4F6))
        ) {
            TriageFlowScreen(
                chatViewModel = chatViewModel,
                showHeader = false,
                onNavigateToMap = onNavigateToMap,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (!showChat) 1f else 0f),
            )
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
                victims = victims,
                onScopeChange = onScopeChange,
                applyStatusBarPadding = false,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (showChat) 1f else 0f),
            )
        }
    }
}

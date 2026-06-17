package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActionButtons(
    isListening: Boolean,
    onTriageClick: () -> Unit,
    onMicStart: () -> Unit,
    onMicStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ─── 音声入力ボタン（長押し）────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isListening) Color(0xFF1A8A96) else Color(0xFF25B1BF)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onMicStart()
                            tryAwaitRelease()
                            onMicStop()
                        }
                    )
                }
        ) {
            Text(
                text = if (isListening) "録音中..." else "音声入力",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        // ─── トリアージ開始ボタン ─────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.5.dp, Color(0xFFEF8080), RoundedCornerShape(14.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onTriageClick() })
                }
        ) {
            Text(
                text = "トリアージ開始",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF8080),
            )
        }
    }
}

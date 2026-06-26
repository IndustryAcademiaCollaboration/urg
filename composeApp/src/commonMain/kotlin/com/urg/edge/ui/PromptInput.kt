package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PromptInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F4F6), RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // プレースホルダー
        if (value.isEmpty()) {
            Text(
                text = "メッセージを入力...",
                fontSize = 15.sp,
                color = Color(0xFFBBBBBB),
                modifier = Modifier.padding(end = 36.dp)
            )
        }
        // 入力フィールド
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = Color(0xFF333333)
            ),
            cursorBrush = SolidColor(Color(0xFF25B1BF)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 36.dp)
        )
        // 送信ボタン（→）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(50))
                .clickable { onSendClick() }
                .padding(4.dp)
        ) {
            Text(
                text = "→",
                fontSize = 20.sp,
                color = Color(0xFF25B1BF),
            )
        }
    }
}

package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_back
import urg.composeapp.generated.resources.ic_mic

@Composable
fun HurryMode(
    onBack: () -> Unit,
    onHistoryClick: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // トップバー
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_back),
                contentDescription = "戻る",
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onBack() }
            )
            Text(
                text = "焦りモード",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF25B1BF),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }

        // 会話履歴ボタン（トップバー下・右寄せ）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .border(1.5.dp, Color(0xFF25B1BF), RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .clickable { onHistoryClick() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "会話履歴",
                    fontSize = 14.sp,
                    color = Color(0xFF25B1BF),
                )
            }
        }

        // マイクボタン（中央）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(250.dp)
                    .shadow(
                        elevation = 48.dp,
                        shape = CircleShape,
                        spotColor = Color(0xFFEF6B6B),
                        ambientColor = Color(0xFFEF6B6B)
                    )
                    .background(Color(0xFFEF6B6B), CircleShape)
                    .clip(CircleShape)
                    .clickable { onMicClick() }
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_mic),
                    contentDescription = "マイク",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(100.dp)
                )
            }
        }
    }
}

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_back

// ─── 会話ロール ───────────────────────────────────────────
private enum class HistoryRole { AI, USER }

// ─── デフォルトデータ ─────────────────────────────────────
private data class HistoryMessage(
    val role: HistoryRole,
    val content: String,
    val timestamp: String,
)

private val defaultHistory = listOf(
    HistoryMessage(HistoryRole.AI,   "患者の状態を教えてください。歩けますか？",     "14:02"),
    HistoryMessage(HistoryRole.USER, "はい、歩けます",                               "14:02"),
    HistoryMessage(HistoryRole.AI,   "呼吸はありますか？",                           "14:03"),
    HistoryMessage(HistoryRole.USER, "はい、呼吸しています",                         "14:03"),
    HistoryMessage(HistoryRole.AI,   "脈はありますか？",                             "14:03"),
    HistoryMessage(HistoryRole.USER, "はい、脈があります",                           "14:04"),
    HistoryMessage(HistoryRole.AI,   "呼びかけに反応しますか？",                     "14:04"),
    HistoryMessage(HistoryRole.USER, "はい、反応しています",                         "14:04"),
    HistoryMessage(HistoryRole.AI,   "判定結果：緑（軽症）\n歩行可能。医療処置は後回しにできます。", "14:04"),
)

// ─── 会話履歴画面 ─────────────────────────────────────────
@Composable
fun ChatHistory(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F6))
    ) {
        // ─── トップバー ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
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
                text = "会話履歴",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF25B1BF),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        HorizontalDivider(color = Color(0xFFE8E8E8))

        // ─── 日時ヘッダー ─────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFDDE1E5), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "2025年5月29日 14:02",
                    fontSize = 11.sp,
                    color = Color(0xFF888888),
                )
            }
        }

        // ─── メッセージリスト ─────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            defaultHistory.forEach { message ->
                Spacer(modifier = Modifier.height(10.dp))
                if (message.role == HistoryRole.AI) {
                    AiBubble(message)
                } else {
                    UserBubble(message)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─── AI バブル（左寄せ） ──────────────────────────────────
@Composable
private fun AiBubble(message: HistoryMessage) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // バブル
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .background(
                        Color.White,
                        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    fontSize = 15.sp,
                    color = Color(0xFF333333),
                    lineHeight = 22.sp,
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = message.timestamp,
                fontSize = 10.sp,
                color = Color(0xFFAAAAAA),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

// ─── ユーザーバブル（右寄せ） ─────────────────────────────
@Composable
private fun UserBubble(message: HistoryMessage) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(horizontalAlignment = Alignment.End) {
            // バブル
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .background(
                        Color(0xFF25B1BF),
                        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    fontSize = 15.sp,
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = message.timestamp,
                fontSize = 10.sp,
                color = Color(0xFFAAAAAA),
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

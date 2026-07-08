package com.urg.edge.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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

private val Teal   = Color(0xFF25B1BF)
private val Green  = Color(0xFF16A36B)
private val Border = Color(0xFFE0E6EA)
private val LabelColor = Color(0xFF10202A)
private val PctActiveColor = Teal
private val PctDoneColor   = Green
private val TrackColor     = Color(0xFFE8EFF3)

/** アプリ起動時のモデル初期化進捗バナー
 *
 * @param llmProgress  0.0〜1.0  (AIモデル)
 * @param sttProgress  0.0〜1.0  (STTモデル)
 * @param ttsProgress  0.0〜1.0  (TTSモデル)
 * @param expanded     true = 3行詳細を表示
 * @param onToggle     ∧/∨ ボタンタップ
 * @param onDismiss    × ボタンタップ
 */
@Composable
fun InitProgressBanner(
    llmProgress: Float,
    sttProgress: Float,
    ttsProgress: Float,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overallProgress = (llmProgress + sttProgress + ttsProgress) / 3f
    val overallPct      = (overallProgress * 100).toInt()
    val allDone         = overallProgress >= 1f

    val dotColor    = if (allDone) Green else Teal
    val headerLabel = "モデルを準備中 ... $overallPct%"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(0.5.dp, Border, RoundedCornerShape(10.dp))
            .background(Color.White)
    ) {
        Column {
            // ── ヘッダー行 ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 状態ドット
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(dotColor, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                // ラベル
                Text(
                    text = headerLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LabelColor,
                    modifier = Modifier.weight(1f),
                )
                // ∧ / ∨ ボタン
                Text(
                    text = if (expanded) "∧" else "∨",
                    fontSize = 14.sp,
                    color = Color(0xFF788E98),
                    modifier = Modifier
                        .clickable(onClick = onToggle)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                Spacer(Modifier.width(4.dp))
                // × ボタン（常に表示）
                Text(
                    text = "×",
                    fontSize = 14.sp,
                    color = Color(0xFF788E98),
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            // ── 展開時：3行プログレスバー ─────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit  = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                ) {
                    ProgressRow("AIモデル",  llmProgress)
                    Spacer(Modifier.height(10.dp))
                    ProgressRow("STTモデル", sttProgress)
                    Spacer(Modifier.height(10.dp))
                    ProgressRow("TTSモデル", ttsProgress)
                }
            }
        }
    }
}

@Composable
private fun ProgressRow(label: String, progress: Float) {
    val pct     = (progress * 100).toInt()
    val done    = progress >= 1f
    val barColor = if (done) Green else Teal
    val pctColor = if (done) PctDoneColor else PctActiveColor

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = LabelColor,
            modifier = Modifier.width(68.dp),
        )
        // プログレスバー
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(TrackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$pct%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = pctColor,
            modifier = Modifier.width(32.dp),
        )
    }
}

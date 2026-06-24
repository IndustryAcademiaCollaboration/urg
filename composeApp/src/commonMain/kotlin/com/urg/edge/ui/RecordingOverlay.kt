package com.urg.edge.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_close
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.abs

// ─── サンプル会話バブル ───────────────────────────────────
private data class BubbleMsg(val isAi: Boolean, val text: String)

private val sampleBubbles = listOf(
    BubbleMsg(true,  "患者の状態を教えてください。歩けますか？"),
    BubbleMsg(false, "はい、歩けます"),
    BubbleMsg(true,  "呼吸はありますか？"),
)

// ─── 録音オーバーレイ画面 ─────────────────────────────────
@Composable
fun RecordingOverlay(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF3A4550))
            .statusBarsPadding()
    ) {
        // ─── 上部：会話履歴ボタン ─────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .align(Alignment.TopEnd)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color(0xFF2C3640), RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .clickable { /* TODO: 会話履歴を表示 */ }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "会話履歴",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }
        }

        // ─── 会話バブルリスト ─────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 64.dp, bottom = 220.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sampleBubbles.forEach { bubble ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (bubble.isAi) Arrangement.Start else Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .background(
                                if (bubble.isAi) Color.White
                                else Color(0xFF25B1BF),
                                RoundedCornerShape(
                                    topStart = if (bubble.isAi) 4.dp else 16.dp,
                                    topEnd   = if (bubble.isAi) 16.dp else 4.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd   = 16.dp,
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = bubble.text,
                            fontSize = 15.sp,
                            color = if (bubble.isAi) Color.Black else Color.White,
                            lineHeight = 22.sp,
                        )
                    }
                }
            }
        }

        // ─── 下部：サウンドウェーブ ＋ 閉じるボタン ──────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            // サウンドウェーブ（Canvas アニメーション）
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(Color(0xFF2C3640), RoundedCornerShape(50))
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                SoundWaveCanvas(
                    modifier = Modifier
                        .width(140.dp)
                        .height(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 閉じるボタン
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .background(Color(0xFFEF6B6B), CircleShape)
                    .clip(CircleShape)
                    .clickable { onClose() }
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_close),
                    contentDescription = "閉じる",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ─── サウンドウェーブ Canvas アニメーション ───────────────
@Composable
private fun SoundWaveCanvas(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase"
    )

    val barCount = 18
    val barColor = Color(0xFF4ECDC4)

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 2 - 1)
        val maxBarHeight = size.height * 0.9f
        val minBarHeight = size.height * 0.15f

        for (i in 0 until barCount) {
            val x = i * barWidth * 2
            // 各バーに異なる位相オフセットを与えて波形を作る
            val offset = i.toFloat() / barCount * 2 * PI.toFloat()
            val normalized = (sin(phase + offset) + 1f) / 2f  // 0..1
            // 中央のバーが高くなるように重み付け
            val centerWeight = 1f - abs((i - barCount / 2f) / (barCount / 2f)) * 0.4f
            val barHeight = (minBarHeight + (maxBarHeight - minBarHeight) * normalized * centerWeight)
            val top = (size.height - barHeight) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

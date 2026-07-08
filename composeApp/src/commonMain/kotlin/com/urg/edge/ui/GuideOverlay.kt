package com.urg.edge.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_gray_close

private val GuideTeal = Color(0xFF25B1BF)

// ── Data ────────────────────────────────────────────────────────────────────

data class GuideTargetRects(
    val inputRect: Rect? = null,
    val voiceRect: Rect? = null,
    val triageRect: Rect? = null,
)

private data class GuideStep(
    val num: String,
    val tag: String,
    val title: String,
    val desc: String,
    val targetKey: String,
)

private val STEPS = listOf(
    GuideStep(
        num = "1", tag = "TEXT INPUT", targetKey = "input",
        title = "テキストで状況を伝える",
        desc = "入力欄に症状やケガの状況を入力し、右の「→」ボタンを押して送信します。",
    ),
    GuideStep(
        num = "2", tag = "VOICE INPUT", targetKey = "voice",
        title = "声で状況を伝える",
        desc = "「音声入力」ボタンを押すと、話した言葉がそのまま文字になります。手がふさがっている時に便利です。",
    ),
    GuideStep(
        num = "3", tag = "SAFETY CHECK", targetKey = "triage",
        title = "安全確認について",
        desc = "「トリアージ開始」を押すと、まず現在地の安全確認が求められます。\n\n・安全な場所にいる場合 → トリアージを開始します\n・危険な場所にいる場合 → 避難マップに切り替わります",
    ),
    GuideStep(
        num = "4", tag = "START TRIAGE", targetKey = "triage",
        title = "トリアージを開始する",
        desc = "状況を伝えたら「トリアージ開始」ボタンを押します。緊急度を判定し、対応の優先順位を表示します。",
    ),
)

// ── Main composable ──────────────────────────────────────────────────────────

@Composable
fun GuideOverlay(
    targetRects: GuideTargetRects,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var stepIndex by remember { mutableStateOf(0) }
    var done      by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val step    = STEPS[stepIndex]
    val isLast  = stepIndex == STEPS.lastIndex

    val targetRect: Rect? = when (step.targetKey) {
        "input"  -> targetRects.inputRect
        "voice"  -> targetRects.voiceRect
        "triage" -> targetRects.triageRect
        else     -> null
    }

    val isInput  = step.targetKey == "input"
    val padPx    = with(density) { (if (isInput) 6.dp else 8.dp).toPx() }
    val cornerPx = with(density) { (if (isInput) 30.dp else 14.dp).toPx() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenHeightPx = with(density) { maxHeight.toPx() }

        // ── Spotlight canvas ─────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color(0x9E141719))
            if (!done && targetRect != null) {
                // Cut spotlight hole
                drawRoundRect(
                    color      = Color.Transparent,
                    topLeft    = Offset(targetRect.left - padPx, targetRect.top - padPx),
                    size       = Size(targetRect.width + padPx * 2, targetRect.height + padPx * 2),
                    cornerRadius = CornerRadius(cornerPx),
                    blendMode  = BlendMode.Clear,
                )
            }
        }

        if (done) {
            // ── Completion card (centered, not full-screen) ──────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .background(Color.White, RoundedCornerShape(20.dp))
                ) {
                    // ─ Close icon (top-right) ─────────────────────────────
                    Icon(
                        painter = painterResource(Res.drawable.ic_gray_close),
                        contentDescription = "閉じる",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                            .size(24.dp)
                            .clickable { onDismiss() }
                    )

                    // ─ Card content ───────────────────────────────────────
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 44.dp, bottom = 28.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .background(GuideTeal, RoundedCornerShape(50))
                        ) {
                            Text("✓", fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "ガイド完了！",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2B2F33),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "これで基本操作はバッチリです。\nいざという時、落ち着いて使いましょう。",
                            fontSize = 14.sp,
                            color = Color(0xFF5A6065),
                            lineHeight = 22.sp,
                        )
                        Spacer(Modifier.height(24.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, Color(0xFFD4D8DA), RoundedCornerShape(12.dp))
                                .clickable { stepIndex = 0; done = false }
                                .padding(vertical = 14.dp)
                        ) {
                            Text("もう一度見る", fontSize = 14.sp, color = Color(0xFF5A6065), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (targetRect != null) {
            // ── Popup card (above spotlight) ─────────────────────────────
            val bottomPx   = screenHeightPx - targetRect.top + with(density) { 18.dp.toPx() }
            val bottomDp   = with(density) { bottomPx.toDp() }

            // Caret horizontal position
            val caretCenterPx = targetRect.left + targetRect.width / 2f
            val cardHPadPx    = with(density) { 16.dp.toPx() }
            val caretLeftPx   = (caretCenterPx - cardHPadPx - with(density) { 7.dp.toPx() })
                .coerceIn(with(density) { 16.dp.toPx() }, with(density) { (maxWidth - 48.dp).toPx() })
            val caretLeftDp   = with(density) { caretLeftPx.toDp() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = bottomDp)
            ) {
                // ── Card ────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    // Badge + tag row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .background(GuideTeal, RoundedCornerShape(9.dp))
                        ) {
                            Text(
                                text = step.num,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        Spacer(Modifier.width(11.dp))
                        Text(
                            text = step.tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = GuideTeal,
                        )
                    }

                    Spacer(Modifier.height(11.dp))

                    Text(
                        text = step.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2B2F33),
                        lineHeight = 24.sp,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = step.desc,
                        fontSize = 14.sp,
                        color = Color(0xFF5A6065),
                        lineHeight = 22.sp,
                    )

                    Spacer(Modifier.height(30.dp))

                    // Progress dots + navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Dots
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            STEPS.forEachIndexed { i, _ ->
                                Box(
                                    modifier = Modifier
                                        .height(8.dp)
                                        .width(if (i == stepIndex) 24.dp else 8.dp)
                                        .background(
                                            color = when {
                                                i == stepIndex -> GuideTeal
                                                i < stepIndex  -> Color(0xFFA9D9D9)
                                                else           -> Color(0xFFD4D8DA)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                        )
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Back button
                        if (stepIndex > 0) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .border(1.5.dp, Color(0xFFD4D8DA), RoundedCornerShape(10.dp))
                                    .clickable { stepIndex-- }
                                    .padding(horizontal = 16.dp, vertical = 9.dp)
                            ) {
                                Text("戻る", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A8084))
                            }
                            Spacer(Modifier.width(10.dp))
                        }

                        // Next / Done button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(GuideTeal, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isLast) done = true else stepIndex++
                                }
                                .padding(horizontal = 20.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = if (isLast) "完了" else "次へ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }

                // ── Caret (rotated square peeking below card) ────────────
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = caretLeftDp)
                        .size(14.dp)
                        .graphicsLayer { rotationZ = 45f }
                        .background(Color.White)
                )
            }
        }
    }
}

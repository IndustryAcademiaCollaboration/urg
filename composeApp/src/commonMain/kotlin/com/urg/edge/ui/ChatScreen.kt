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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urg.edge.ChatScope
import com.urg.edge.ChatUiState
import com.urg.edge.Strings
import com.urg.edge.VictimRecord
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

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
    victims: List<VictimRecord> = emptyList(),
    onScopeChange: (ChatScope) -> Unit = {},
    applyStatusBarPadding: Boolean = true,
    modifier: Modifier = Modifier,
) {
    // 送信保留フラグ：STT完了後に自動送信
    var pendingSend by remember { mutableStateOf(false) }

    // 録音タイマー
    var recordSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(uiState.isListening) {
        if (uiState.isListening) {
            recordSeconds = 0
            while (true) {
                delay(1000)
                recordSeconds++
            }
        }
    }

    // STT完了後に送信
    LaunchedEffect(uiState.isListening, uiState.promptText) {
        if (pendingSend && !uiState.isListening && uiState.promptText.isNotBlank()) {
            onSendClick()
            pendingSend = false
        }
    }

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

        if (uiState.isListening) {
            // ── 録音パネル ─────────────────────────────────────────
            VoiceRecordingPanel(
                promptText = uiState.promptText,
                recordSeconds = recordSeconds,
                amplitude = uiState.micAmplitude,
                onPromptChange = onPromptChange,
                onCancel = {
                    onMicStop()
                    onPromptChange("")
                },
                onSend = {
                    pendingSend = true
                    onMicStop()
                },
            )
        } else {
            // ── 通常の入力エリア ────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (victims.isNotEmpty()) {
                    ScopeChipRow(
                        victims = victims,
                        selectedScope = uiState.chatScope,
                        onScopeChange = onScopeChange,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

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
}

// ── 会話スコープ選択チップ ─────────────────────────────────────────────────────

@Composable
private fun ScopeChipRow(
    victims: List<VictimRecord>,
    selectedScope: ChatScope,
    onScopeChange: (ChatScope) -> Unit,
) {
    var customPickerExpanded by remember(selectedScope is ChatScope.Custom) {
        mutableStateOf(selectedScope is ChatScope.Custom)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatScope.presets.forEach { scope ->
                val count = victims.count { scope.matches(it) }
                val selected = scope == selectedScope
                ScopeChip(
                    label = "${scope.label}($count)",
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        customPickerExpanded = false
                        // 選択中のチップを再タップしたら解除（全員対象に戻る）
                        onScopeChange(if (selected) ChatScope.All else scope)
                    },
                )
            }
            val customScope = selectedScope as? ChatScope.Custom
            ScopeChip(
                label = if (customScope != null) "個別(${customScope.displayNos.size})" else "個別",
                selected = customScope != null,
                modifier = Modifier.weight(1f),
                onClick = { customPickerExpanded = !customPickerExpanded },
            )
        }

        if (customPickerExpanded) {
            Spacer(modifier = Modifier.height(6.dp))
            val selectedNos = (selectedScope as? ChatScope.Custom)?.displayNos ?: emptySet()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                victims.sortedBy { it.displayNo }.forEach { v ->
                    val checked = v.displayNo in selectedNos
                    ScopeChip(
                        label = "P${v.displayNo}",
                        selected = checked,
                        compact = true,
                        onClick = {
                            val next = if (checked) selectedNos - v.displayNo else selectedNos + v.displayNo
                            onScopeChange(if (next.isEmpty()) ChatScope.All else ChatScope.Custom(next))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScopeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color(0xFF25B1BF) else Color(0xFFF0F0F0))
            .clickable { onClick() }
            .then(if (compact) Modifier.padding(horizontal = 12.dp) else Modifier)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF888888),
        )
    }
}

// ── 録音パネル ─────────────────────────────────────────────────────────────────

@Composable
private fun VoiceRecordingPanel(
    promptText: String,
    recordSeconds: Int,
    amplitude: Float,
    onPromptChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    val minutes = recordSeconds / 60
    val seconds = recordSeconds % 60
    val timerText = "$minutes:${seconds.toString().padStart(2, '0')}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color(0xFF1A2D38))
    ) {
        // ハンドルバー
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .align(Alignment.CenterHorizontally)
                .background(Color(0x33FFFFFF), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(10.dp))

        // 録音中ラベル + タイマー
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val dotTransition = rememberInfiniteTransition(label = "dot")
                val dotAlpha by dotTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "dotAlpha",
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Color(0xFFFF6366).copy(alpha = dotAlpha), CircleShape)
                )
                Text(
                    text = "REC",
                    fontSize = 12.sp,
                    color = Color(0xFFFF6366),
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = timerText,
                fontSize = 12.sp,
                color = Color(0x66FFFFFF),
            )
        }

        Spacer(Modifier.height(10.dp))

        // 文字起こしカード（タップで編集可能）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x14FFFFFF))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column {
                // 録音中・認識後どちらでも常にタップ・編集できる
                BasicTextField(
                    value = promptText,
                    onValueChange = onPromptChange,
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 20.sp,
                    ),
                    cursorBrush = SolidColor(Color(0xFF25B1BF)),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (promptText.isBlank()) {
                                Text(
                                    text = "認識中...",
                                    fontSize = 13.sp,
                                    color = Color(0x66FFFFFF),
                                    lineHeight = 20.sp,
                                )
                            }
                            inner()
                        }
                    }
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "タップして修正できます",
                    fontSize = 10.sp,
                    color = Color(0x44FFFFFF),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }

        // 声紋 Canvas（中央寄せ・上下余白多め）
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            VoiceWaveCanvas(
                amplitude = amplitude,
                modifier = Modifier
                    .width(220.dp)
                    .height(60.dp)
            )
        }
        Spacer(Modifier.height(14.dp))

        // ボタン行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // キャンセル（赤丸 ✕）
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFFF5C5C), CircleShape)
                    .clip(CircleShape)
                    .clickable { onCancel() }
            ) {
                Text("✕", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            // 送信ボタン
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF25B1BF))
                    .clickable { onSend() }
            ) {
                Text(
                    text = "送信 →",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

// ── 声紋 Canvas アニメーション ─────────────────────────────────────────────────

@Composable
private fun VoiceWaveCanvas(amplitude: Float = 0f, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    // 速い波（メイン）
    val phase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase1",
    )
    // 遅い波（サブ：有機的な揺らぎを加える）
    val phase2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase2",
    )

    val barCount = 18
    val barColor = Color(0xFFFF6366)

    // 音量を 0..1 に正規化（通常会話の RMS は 0.02〜0.2 程度）
    val ampNorm = (amplitude / 0.18f).coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val barW    = 3.5.dp.toPx()
        val gap     = 3.5.dp.toPx()
        val totalW  = barCount * (barW + gap) - gap
        val startX  = (size.width - totalW) / 2f

        // 無音時は最小高さのみ表示し、発話に応じて大きくなる
        val minBarH = size.height * 0.06f
        val maxBarH = size.height * (0.12f + 0.83f * ampNorm)

        for (i in 0 until barCount) {
            val x   = startX + i * (barW + gap)
            val t   = i.toFloat() / (barCount - 1)
            val off = t * 2f * PI.toFloat()

            // 2つのサイン波を合成してより有機的な動きに
            val wave1    = sin(phase1 + off * 2.2f)
            val wave2    = sin(phase2 + off * 1.3f + 0.9f)
            val combined = (wave1 * 0.65f + wave2 * 0.35f + 1f) / 2f  // 0..1

            // 中央が高くなるベルカーブ
            val bell = 1f - (abs(t - 0.5f) * 2f) * 0.28f

            val barH = minBarH + (maxBarH - minBarH) * combined * bell
            val top  = (size.height - barH) / 2f

            drawRoundRect(
                color        = barColor,
                topLeft      = Offset(x, top),
                size         = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f),
            )
        }
    }
}

package com.urg.edge.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
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
import com.urg.edge.TriageResult
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
    var pendingSend by remember { mutableStateOf(false) }

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
                    Spacer(modifier = Modifier.height(10.dp))
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
                                text = "\u2713  ${Strings.BUTTON_YES}",
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
                                text = "\u2715  ${Strings.BUTTON_NO}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD94444),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 入力行：テキスト入力
                PromptInput(
                    value = uiState.promptText,
                    onValueChange = onPromptChange,
                    onSendClick = onSendClick,
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 音声入力ボタン
                ActionButtons(
                    isListening = uiState.isListening,
                    onVoiceInputClick = onVoiceInputClick,
                )
            }
        }
    }
}

// スコープ選択 UI

@Composable
private fun ScopeChipRow(
    victims: List<VictimRecord>,
    selectedScope: ChatScope,
    onScopeChange: (ChatScope) -> Unit,
) {
    val severeCount = victims.count { it.result == TriageResult.SEVERE }
    val minorCount  = victims.count { it.result == TriageResult.MINOR }
    val customScope = selectedScope as? ChatScope.Custom
    var indExpanded by remember { mutableStateOf(customScope != null) }

    LaunchedEffect(selectedScope) {
        if (selectedScope !is ChatScope.Custom) indExpanded = false
    }

    val hasActiveScope = selectedScope !is ChatScope.All

    Column {
        // クリアボタン行
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            if (hasActiveScope) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onScopeChange(ChatScope.All)
                            indExpanded = false
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "×",
                        fontSize = 12.sp,
                        color = Color(0xFFB0BAC2),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "クリア",
                        fontSize = 12.sp,
                        color = Color(0xFFB0BAC2),
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // スコープチップ列
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScopeChip(
                label = "全員",
                selected = selectedScope is ChatScope.All,
                onClick = {
                    onScopeChange(ChatScope.All)
                    indExpanded = false
                }
            )
            ScopeChip(
                label = "重症 ($severeCount)",
                selected = selectedScope is ChatScope.Severe,
                severityDotColor = if (selectedScope !is ChatScope.Severe) Color(0xFFE5463F) else null,
                selectedColor = Color(0xFFE5463F),
                onClick = {
                    onScopeChange(if (selectedScope is ChatScope.Severe) ChatScope.All else ChatScope.Severe)
                    indExpanded = false
                }
            )
            ScopeChip(
                label = "軽症 ($minorCount)",
                selected = selectedScope is ChatScope.Minor,
                severityDotColor = if (selectedScope !is ChatScope.Minor) Color(0xFF16A36B) else null,
                selectedColor = Color(0xFF16A36B),
                onClick = {
                    onScopeChange(if (selectedScope is ChatScope.Minor) ChatScope.All else ChatScope.Minor)
                    indExpanded = false
                }
            )
            ScopeChip(
                label = if (customScope != null) "個別(${customScope.displayNos.size})" else "個別",
                selected = customScope != null || indExpanded,
                onClick = { indExpanded = !indExpanded }
            )
        }

        // 患者チップ（個別モード）
        if (indExpanded) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                victims.sortedBy { it.displayNo }.forEach { v ->
                    val isSevere = v.result == TriageResult.SEVERE
                    val checked  = v.displayNo in (customScope?.displayNos ?: emptySet())
                    PatientChip(
                        label    = "P${v.displayNo}",
                        isSevere = isSevere,
                        selected = checked,
                        onClick  = {
                            val nos  = customScope?.displayNos ?: emptySet()
                            val next = if (checked) nos - v.displayNo else nos + v.displayNo
                            onScopeChange(if (next.isEmpty()) ChatScope.All else ChatScope.Custom(next))
                        }
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
    severityDotColor: Color? = null,
    selectedColor: Color = Color(0xFF25B1BF),
) {
    val bgColor     = if (selected) selectedColor else Color(0xFFF7F8FA)
    val borderColor = if (selected) selectedColor else Color(0xFFDDE3E8)
    val textColor   = if (selected) Color.White else Color(0xFF8C9BA5)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .border(1.5.dp, borderColor, RoundedCornerShape(50))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp)
    ) {
        if (severityDotColor != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(severityDotColor, CircleShape)
            )
        }
        Text(
            text       = label,
            fontSize   = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color      = textColor,
        )
    }
}

@Composable
private fun PatientChip(
    label: String,
    isSevere: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val dotColor    = if (isSevere) Color(0xFFE5463F) else Color(0xFF16A36B)
    val borderColor = when {
        selected && isSevere  -> Color(0xFFE5463F)
        selected && !isSevere -> Color(0xFF16A36B)
        else                  -> Color(0xFFDDE3E8)
    }
    val bgColor = when {
        selected && isSevere  -> Color(0xFFFFF4F4)
        selected && !isSevere -> Color(0xFFF0FAF5)
        else                  -> Color(0xFFF7F8FA)
    }
    val textColor = when {
        selected && isSevere  -> Color(0xFFC62828)
        selected && !isSevere -> Color(0xFF0E7A50)
        else                  -> Color(0xFF8C9BA5)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .border(1.5.dp, borderColor, RoundedCornerShape(50))
            .background(bgColor)
            .clickable { onClick() }
            .padding(start = 10.dp, end = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(dotColor, CircleShape)
        )
        Text(
            text       = label,
            fontSize   = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color      = textColor,
        )
    }
}

// 録音パネル

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
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .align(Alignment.CenterHorizontally)
                .background(Color(0x33FFFFFF), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(10.dp))

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x14FFFFFF))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column {
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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

// 声紋 Canvas アニメーション

@Composable
private fun VoiceWaveCanvas(amplitude: Float = 0f, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase1",
    )
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
    val ampNorm = (amplitude / 0.18f).coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val barW   = 3.5.dp.toPx()
        val gap    = 3.5.dp.toPx()
        val totalW = barCount * (barW + gap) - gap
        val startX = (size.width - totalW) / 2f

        val minBarH = size.height * 0.06f
        val maxBarH = size.height * (0.12f + 0.83f * ampNorm)

        for (i in 0 until barCount) {
            val x   = startX + i * (barW + gap)
            val t   = i.toFloat() / (barCount - 1)
            val off = t * 2f * PI.toFloat()

            val wave1    = sin(phase1 + off * 2.2f)
            val wave2    = sin(phase2 + off * 1.3f + 0.9f)
            val combined = (wave1 * 0.65f + wave2 * 0.35f + 1f) / 2f

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

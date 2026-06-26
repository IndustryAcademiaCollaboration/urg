package com.urg.edge.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urg.edge.TriageResult
import com.urg.edge.TriageStep
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_back
import urg.composeapp.generated.resources.ic_gray_close
import urg.composeapp.generated.resources.ic_location

// ─── Colors ───────────────────────────────────────────────────────────────────
private val Teal     = Color(0xFF25B1BF)
private val TealBg   = Color(0xFFE8F9FA)
private val Green    = Color(0xFF1D9E75)
private val GreenBg  = Color(0xFFE1F5EE)
private val Red      = Color(0xFFE24B4A)
private val RedBg    = Color(0xFFFCEBEB)
private val Orange   = Color(0xFFEF9F27)
private val OrangeBg = Color(0xFFFAEEDA)
private val GrayLine = Color(0xFFCCCCCC)
private val GrayBg   = Color(0xFFF5F5F5)
private val GrayText = Color(0xFF9AACB4)
private val BodyText = Color(0xFF2A3A45)
private val DarkGrayText = Color(0xFF737678)

// ─── Data ─────────────────────────────────────────────────────────────────────
private enum class YesDir { DOWN, RIGHT }

private data class StepInfo(
    val step: TriageStep,
    val num: String,
    val label: String,
    val question: String,
    val yesDir: YesDir,
    val yesResultId: String?,   // non-null when yes leads to leaf result
    val noResultId: String?,    // null for WALK (no goes down)
)

private val FLOW_STEPS = listOf(
    StepInfo(TriageStep.WALK,         "①", "歩行確認",   "自力で歩けますか？",         YesDir.RIGHT, "minor",         null),
    StepInfo(TriageStep.BREATHING,    "②", "呼吸確認",   "呼吸はありますか？",         YesDir.DOWN,  null,            "severe_airway"),
    StepInfo(TriageStep.CIRCULATION,  "③", "循環確認",   "脈はありますか？",           YesDir.DOWN,  null,            "severe_circ"),
    StepInfo(TriageStep.CONSCIOUSNESS,"④", "意識確認",   "呼びかけに反応しますか？",   YesDir.RIGHT, "severe_injury", "severe_cons"),
)

private data class ResultInfo(
    val label: String,
    val subLines: List<String>,
    val dot: Color,
    val bg: Color,
    val border: Color,
)

private val FLOW_RESULTS = mapOf(
    "danger"        to ResultInfo("退避指示", listOf("安全確保・避難"),         Orange, OrangeBg, Orange),
    "minor"         to ResultInfo("軽症",     listOf("救護所へ自力で"),         Green,  GreenBg,  Green),
    "severe_airway" to ResultInfo("重症",     listOf("気道確保・CPR", "AED使用"), Red, RedBg, Red),
    "severe_circ"   to ResultInfo("重症",     listOf("出血を圧迫", "圧迫を保持"), Red, RedBg, Red),
    "severe_cons"   to ResultInfo("重症",     listOf("回復体位", "呼吸を確認"), Red, RedBg, Red),
    "severe_injury" to ResultInfo("重症",     listOf("出血箇所を圧迫", "・安静"), Red, RedBg, Red),
)

// ─── LLM Messages (TODO: replace with actual ChatViewModel call) ──────────────
private val LLM_MESSAGES = mapOf(
    "danger"        to "周囲が危険な状況です。まず自身の安全を確保し、速やかに安全な場所へ避難してください。救助活動は安全を確認してから実施してください。",
    "minor"         to "患者は自力歩行が可能な軽症と判断されます。近くの救護所へ誘導し、症状の変化を引き続き観察してください。",
    "severe_airway" to "呼吸停止の疑いがあります。直ちに気道を確保し、必要であればCPRを開始してください。AEDが利用可能な場合は速やかに使用してください。",
    "severe_circ"   to "循環障害の可能性があります。出血部位をタオルなどで強く圧迫し、その状態を保持してください。救急隊の到着を待ちながら観察を続けてください。",
    "severe_cons"   to "意識障害があります。患者を回復体位（横向き）に置き、呼吸状態を継続的に確認してください。嘔吐による窒息に注意してください。",
    "severe_injury" to "重篤な外傷の可能性があります。出血箇所を強く圧迫止血し、患者を安静に保ってください。体を動かさないよう注意してください。",
)

// ─── Flow State ───────────────────────────────────────────────────────────────
private data class FlowState(
    val answers: Map<TriageStep, Boolean> = emptyMap(),
    val currentStep: TriageStep? = null,      // null = 安全確認前（未開始）
    val reachedResult: String? = null,
    val finalResult: TriageResult? = null,
)

private fun FlowState.answer(yes: Boolean): FlowState {
    val step = currentStep ?: return this
    val next = answers + (step to yes)
    return when {
        step == TriageStep.WALK && yes ->
            copy(answers = next, currentStep = null, reachedResult = "minor", finalResult = TriageResult.MINOR)
        step == TriageStep.WALK && !yes ->
            copy(answers = next, currentStep = TriageStep.BREATHING)
        step == TriageStep.BREATHING && !yes ->
            copy(answers = next, currentStep = null, reachedResult = "severe_airway", finalResult = TriageResult.SEVERE)
        step == TriageStep.BREATHING && yes ->
            copy(answers = next, currentStep = TriageStep.CIRCULATION)
        step == TriageStep.CIRCULATION && !yes ->
            copy(answers = next, currentStep = null, reachedResult = "severe_circ", finalResult = TriageResult.SEVERE)
        step == TriageStep.CIRCULATION && yes ->
            copy(answers = next, currentStep = TriageStep.CONSCIOUSNESS)
        step == TriageStep.CONSCIOUSNESS && !yes ->
            copy(answers = next, currentStep = null, reachedResult = "severe_cons", finalResult = TriageResult.SEVERE)
        step == TriageStep.CONSCIOUSNESS && yes ->
            copy(answers = next, currentStep = null, reachedResult = "severe_injury", finalResult = TriageResult.SEVERE)
        else -> this
    }
}

// ─── Main Screen ──────────────────────────────────────────────────────────────
@Composable
fun TriageFlowScreen(
    onBack: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var state            by remember { mutableStateOf(FlowState()) }
    var showModal        by remember { mutableStateOf(false) }
    var showSafetyPreCheck by remember { mutableStateOf(true) }   // 最初の安全確認
    var showDangerModal  by remember { mutableStateOf(false) }    // 危険時のモーダル

    // Open modal automatically when diagnosis completes
    LaunchedEffect(state.reachedResult) {
        if (state.reachedResult != null) showModal = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F6))
    ) {
        // ── Main content ──
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──
            if (showHeader) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
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
                        text = "トリアージ",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Teal,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // ── Flowchart area ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 8.dp, end = 8.dp, top = 36.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FLOW_STEPS.forEachIndexed { idx, info ->
                    val ans = state.answers[info.step]
                    val nodeState: String = when {
                        ans == true  -> "yes"
                        ans == false -> "no"
                        state.currentStep == info.step -> "active"
                        else -> "idle"
                    }

                    FlowStepRow(
                        info          = info,
                        nodeState     = nodeState,
                        reachedResult = state.reachedResult,
                    )

                    // Vertical arrow between steps
                    if (idx < FLOW_STEPS.size - 1) {
                        val downIsYes  = info.yesDir == YesDir.DOWN
                        val activeDown = (downIsYes && nodeState == "yes") || (!downIsYes && nodeState == "no")
                        val downColor  = when {
                            activeDown && downIsYes  -> Green
                            activeDown && !downIsYes -> Red
                            else                     -> GrayLine
                        }
                        val downLabel = if (downIsYes) "はい" else "いいえ"

                        VerticalArrowSection(
                            color      = downColor,
                            label      = downLabel,
                            labelColor = if (activeDown) downColor else GrayText,
                            isDashed   = !activeDown,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Bottom panel ──
            when {
                // ── ① 安全確認プレチェック（最初に表示） ─────────
                showSafetyPreCheck -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "あなた自身は安全ですか？",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = BodyText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 安全ボタン（緑）
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFE8FAF3))
                                    .clickable {
                                        showSafetyPreCheck = false
                                        state = state.copy(currentStep = TriageStep.WALK)
                                    }
                            ) {
                                Text("✓  安全", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E9E6E))
                            }
                            // 危険ボタン（赤）
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(RedBg)
                                    .clickable { showDangerModal = true }
                            ) {
                                Text("⚠  危険", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Red)
                            }
                        }
                    }
                }
                // ── ② トリアージ質問（通常フロー） ───────────────
                state.currentStep != null -> {
                    val info = FLOW_STEPS.find { it.step == state.currentStep }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        if (info != null) {
                            Text(
                                text = info.question,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = BodyText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )
                        }
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
                                    .clickable { state = state.answer(true) }
                            ) {
                                Text("✓  はい", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E9E6E))
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFFFF0F0))
                                    .clickable { state = state.answer(false) }
                            ) {
                                Text("✕  いいえ", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD94444))
                            }
                        }
                    }
                }
                // ── ③ 結果確認ストリップ ─────────────────────────
                state.reachedResult != null && !showModal -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(50))
                                .background(TealBg)
                                .clickable { showModal = true }
                        ) {
                            Text("結果を確認", fontSize = 13.sp, color = Teal, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(50))
                                .background(GrayBg)
                                .clickable {
                                    state = FlowState()
                                    showSafetyPreCheck = true
                                }
                        ) {
                            Text("↺ やり直す", fontSize = 13.sp, color = DarkGrayText)
                        }
                    }
                }
            }
        }

        // ── 危険モーダル（安全確認で「危険」を選択時） ────────
        if (showDangerModal) {
            // Dim background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(enabled = false) { }
            )
            // Modal card
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    // ── Modal header ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Orange)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "退避指示",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Orange,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "安全確保・避難",
                                fontSize = 13.sp,
                                color = Orange.copy(alpha = 0.75f),
                            )
                        }
                        Icon(
                            painter = painterResource(Res.drawable.ic_gray_close),
                            contentDescription = "閉じる",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { showDangerModal = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── 説明テキスト ──
                    Text(
                        text = "周囲が危険な状況です。まず自身の安全を確保し、速やかに安全な場所へ避難してください。\n\n現在周囲に危険がない場合は、できるだけ早く安全な場所へ移動してください。救助活動は安全を確認してから実施してください。",
                        fontSize = 15.sp,
                        color = BodyText,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── 地図へ移動ボタン ──
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Teal)
                            .clickable {
                                showDangerModal = false
                                onNavigateToMap()
                            }
                            .padding(vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_location),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "地図で避難場所を確認",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        // ── Result modal overlay ──
        if (showModal && state.reachedResult != null) {
            val res    = FLOW_RESULTS[state.reachedResult!!]
            val llmMsg = LLM_MESSAGES[state.reachedResult!!] ?: ""

            if (res != null) {
                // Dim background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(enabled = false) { }
                )

                // Modal card
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(20.dp)
                    ) {
                        // ── Modal header: result label + X button ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(res.dot)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = res.label,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = res.dot,
                                )
                                if (res.subLines.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = res.subLines.first(),
                                        fontSize = 13.sp,
                                        color = res.dot.copy(alpha = 0.75f),
                                    )
                                }
                            }
                            // X close button
                            Icon(
                                painter = painterResource(Res.drawable.ic_gray_close),
                                contentDescription = "閉じる",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { showModal = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── LLM response area ──
                        Text(
                            text = llmMsg,
                            fontSize = 16.sp,
                            color = BodyText,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        // ── Restart button ──
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0F0F0))
                                .clickable {
                                    state = FlowState()
                                    showModal = false
                                    showSafetyPreCheck = true
                                }
                                .padding(vertical = 14.dp)
                        ) {
                            Text(
                                text = "↺  最初からやり直す",
                                fontSize = 14.sp,
                                color = DarkGrayText,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Flow Step Row ────────────────────────────────────────────────────────────
@Composable
private fun FlowStepRow(
    info: StepInfo,
    nodeState: String,
    reachedResult: String?,
) {
    val hasLeftResult  = info.noResultId != null               // いいえ goes to left result
    val hasRightResult = info.yesDir == YesDir.RIGHT && info.yesResultId != null

    val leftActive  = nodeState == "no"  && hasLeftResult
    val rightActive = nodeState == "yes" && hasRightResult

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left result node (88dp)
        Box(modifier = Modifier.width(88.dp)) {
            if (hasLeftResult) {
                val res     = FLOW_RESULTS[info.noResultId ?: ""] ?: return@Box
                val reached = reachedResult == info.noResultId
                SmallResultNode(
                    res     = res,
                    reached = reached,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Left arrow (question → left result)
        Box(modifier = Modifier.width(28.dp).height(40.dp)) {
            if (hasLeftResult) {
                DashedHorizontalArrow(
                    color    = if (leftActive || reachedResult == info.noResultId) Red else GrayLine,
                    label    = "いいえ",
                    toLeft   = true,
                    isActive = leftActive || reachedResult == info.noResultId,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Question node (flexible center)
        QuestionNode(
            info      = info,
            nodeState = nodeState,
            modifier  = Modifier.weight(1f),
        )

        // Right arrow (question → right result)
        Box(modifier = Modifier.width(28.dp).height(40.dp)) {
            if (hasRightResult) {
                DashedHorizontalArrow(
                    color    = if (rightActive || reachedResult == info.yesResultId) Green else GrayLine,
                    label    = "はい",
                    toLeft   = false,
                    isActive = rightActive || reachedResult == info.yesResultId,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Right result node (88dp)
        Box(modifier = Modifier.width(88.dp)) {
            if (hasRightResult) {
                val res     = FLOW_RESULTS[info.yesResultId ?: ""] ?: return@Box
                val reached = reachedResult == info.yesResultId
                SmallResultNode(
                    res     = res,
                    reached = reached,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ─── Question Node ────────────────────────────────────────────────────────────
private data class NodeStyle(
    val bg: Color,
    val border: Color,
    val borderW: Float,
    val numBg: Color,
    val titleColor: Color,
    val subColor: Color,
)

private fun nodeStyle(nodeState: String) = when (nodeState) {
    "active" -> NodeStyle(TealBg,      Teal,     2.5f, Teal,     Teal,     Color(0xFF0B7A8A))
    "yes"    -> NodeStyle(GreenBg,     Green,    2.0f, Green,    Green,    Green)
    "no"     -> NodeStyle(GrayBg,      GrayLine, 1.5f, GrayLine, GrayText, GrayText)
    else     -> NodeStyle(Color.White, GrayLine, 1.0f, GrayLine, GrayText, GrayText)
}

@Composable
private fun QuestionNode(
    info: StepInfo,
    nodeState: String,
    modifier: Modifier = Modifier,
) {
    val s = nodeStyle(nodeState)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(width = s.borderW.dp, color = s.border, shape = RoundedCornerShape(14.dp))
            .background(s.bg)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(s.numBg)
            ) {
                Text(
                    text       = info.num,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text       = info.label,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = s.titleColor,
                )
                Text(
                    text     = info.question,
                    fontSize = 11.sp,
                    color    = s.subColor,
                )
            }
        }
    }
}

// ─── Small Result Node ────────────────────────────────────────────────────────
@Composable
private fun SmallResultNode(
    res: ResultInfo,
    reached: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg     = if (reached) res.bg     else GrayBg
    val border = if (reached) res.border else GrayLine
    val dot    = if (reached) res.dot    else GrayLine
    val tc     = if (reached) res.dot    else GrayText

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(width = if (reached) 2.dp else 1.dp, color = border, shape = RoundedCornerShape(10.dp))
            .background(bg)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(dot))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = res.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tc)
            }
            res.subLines.forEach { line ->
                Text(text = line, fontSize = 9.sp, color = tc, lineHeight = 13.sp)
            }
        }
    }
}

// ─── Dashed Horizontal Arrow ──────────────────────────────────────────────────
@Composable
private fun DashedHorizontalArrow(
    color: Color,
    label: String,
    toLeft: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val dash = if (isActive) null else PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val midY = size.height / 2f + 4.dp.toPx()  // slightly below center (label above)
            drawLine(
                color       = color,
                start       = Offset(0f, midY),
                end         = Offset(size.width, midY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect  = dash,
            )
            // Arrowhead
            val arrowSize = 5.dp.toPx()
            if (toLeft) {
                // Arrow pointing left (at x=0)
                drawLine(color = color, start = Offset(arrowSize, midY - arrowSize), end = Offset(0f, midY), strokeWidth = 1.5.dp.toPx())
                drawLine(color = color, start = Offset(arrowSize, midY + arrowSize), end = Offset(0f, midY), strokeWidth = 1.5.dp.toPx())
            } else {
                // Arrow pointing right (at x=width)
                drawLine(color = color, start = Offset(size.width - arrowSize, midY - arrowSize), end = Offset(size.width, midY), strokeWidth = 1.5.dp.toPx())
                drawLine(color = color, start = Offset(size.width - arrowSize, midY + arrowSize), end = Offset(size.width, midY), strokeWidth = 1.5.dp.toPx())
            }
        }
        // Label
        Text(
            text      = label,
            fontSize  = 8.sp,
            color     = color,
            textAlign = TextAlign.Center,
            modifier  = Modifier.align(Alignment.TopCenter).padding(top = 2.dp),
        )
    }
}

// ─── Vertical Arrow Section ───────────────────────────────────────────────────
@Composable
private fun VerticalArrowSection(
    color: Color,
    label: String,
    labelColor: Color,
    isDashed: Boolean,
) {
    val side = 116.dp  // left result(88) + arrow(28) = side column width

    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(side))
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val x   = size.width / 2f
                val dash = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f) else null
                drawLine(
                    color       = color,
                    start       = Offset(x, 0f),
                    end         = Offset(x, size.height - 8.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect  = dash,
                )
                // Arrowhead pointing down
                val ay  = size.height - 4.dp.toPx()
                val aw  = 5.dp.toPx()
                drawLine(color = color, start = Offset(x - aw, ay - aw), end = Offset(x, ay), strokeWidth = 1.5.dp.toPx())
                drawLine(color = color, start = Offset(x + aw, ay - aw), end = Offset(x, ay), strokeWidth = 1.5.dp.toPx())
            }
            Text(
                text     = label,
                fontSize = 9.sp,
                color    = labelColor,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(start = 14.dp),
            )
        }
        Spacer(modifier = Modifier.width(side))
    }
}

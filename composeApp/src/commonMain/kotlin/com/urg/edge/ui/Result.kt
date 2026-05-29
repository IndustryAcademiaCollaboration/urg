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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_back

// ─── トリアージ判定ロジック ───────────────────────────────
enum class TriageLevel(
    val labelColor: Color,
    val shadowColor: Color,
    val colorLabel: String,
    val severityLabel: String,
    val description: String,
) {
    GREEN(
        Color(0xFF4CAF50), Color(0xFF69F0AE),
        "緑", "軽症",
        "歩行可能。医療処置は後回しにできます。"
    ),
    YELLOW(
        Color(0xFFFFB300), Color(0xFFFFE57F),
        "黄", "中等症",
        "歩行困難ですが、生命の危険は直接的ではありません。"
    ),
    RED(
        Color(0xFFEF5350), Color(0xFFFF8A80),
        "赤", "重症",
        "生命の危険があり、緊急に処置が必要です。"
    ),
    BLACK(
        Color(0xFF616161), Color(0xFFBDBDBD),
        "黒", "死亡・救命困難",
        "呼吸がなく、救命は困難な状態です。"
    ),
}

fun classifyTriage(answers: List<Boolean>): TriageLevel {
    val canWalk    = answers.getOrElse(0) { false }
    val breathing  = answers.getOrElse(1) { false }
    val pulse      = answers.getOrElse(2) { false }
    val responsive = answers.getOrElse(3) { false }

    return when {
        canWalk       -> TriageLevel.GREEN
        !breathing    -> TriageLevel.BLACK
        !pulse        -> TriageLevel.RED
        !responsive   -> TriageLevel.RED
        else          -> TriageLevel.YELLOW
    }
}

// ─── 質問ラベル ──────────────────────────────────────────
private val questionLabels = listOf(
    "歩くことはできますか？",
    "呼吸はありますか？",
    "脈はありますか？",
    "呼びかけに反応しますか？",
)

// ─── Result 画面 ─────────────────────────────────────────
@Composable
fun Result(
    answers: List<Boolean>,
    onBack: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val level = classifyTriage(answers)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
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
                text = "診断結果",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF25B1BF),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── 回答内容セクション ──
        Text(
            text = "回答内容",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF788E98),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        questionLabels.forEachIndexed { index, question ->
            val answer = answers.getOrElse(index) { false }
            QuestionAnswerRow(
                number = index + 1,
                question = question,
                answer = answer,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            if (index < questionLabels.size - 1) {
                HorizontalDivider(
                    color = Color(0xFFF0F0F0),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider(color = Color(0xFFF0F0F0))

        Spacer(modifier = Modifier.height(32.dp))

        // ── 判定結果セクション ──
        Text(
            text = "判定結果",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF788E98),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 結果カード
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = level.shadowColor,
                    ambientColor = level.shadowColor
                )
                .background(Color.White, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 重症度ラベル
                Text(
                    text = level.severityLabel,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = level.labelColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 説明
                Text(
                    text = level.description,
                    fontSize = 14.sp,
                    color = Color(0xFF788E98),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ホームに戻るボタン
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .background(Color(0xFF25B1BF), RoundedCornerShape(16.dp))
                .clickable { onHome() }
                .padding(vertical = 18.dp)
        ) {
            Text(
                text = "ホームに戻る",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─── Q&A 行 ──────────────────────────────────────────────
@Composable
private fun QuestionAnswerRow(
    number: Int,
    question: String,
    answer: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // 番号バッジ
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .background(Color(0xFF25B1BF), RoundedCornerShape(50))
            ) {
                Text(
                    text = "$number",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = question,
                fontSize = 15.sp,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )
        }
        // はい / いいえ バッジ
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(
                    if (answer) Color(0xFFEAF7F0) else Color(0xFFFFF0EE),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (answer) "はい" else "いいえ",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (answer) Color(0xFF4CAF50) else Color(0xFFEF5350)
            )
        }
    }
}

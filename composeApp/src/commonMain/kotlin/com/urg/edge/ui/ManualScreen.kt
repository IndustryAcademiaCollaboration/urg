package com.urg.edge.ui

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_chat
import urg.composeapp.generated.resources.ic_gray_close
import urg.composeapp.generated.resources.ic_manual
import urg.composeapp.generated.resources.ic_map_selected
import urg.composeapp.generated.resources.ic_more_selected
import urg.composeapp.generated.resources.ic_place
import urg.composeapp.generated.resources.ic_priority_selected
import urg.composeapp.generated.resources.ic_settings
import urg.composeapp.generated.resources.ic_tag
import urg.composeapp.generated.resources.ic_triage_selected

// ── カラー ───────────────────────────────────────────────────────────────

private val ManualTeal     = Color(0xFF25B1BF)
private val ManualDarkBg   = Color(0xFF141719)
private val ManualInk      = Color(0xFF10202A)
private val ManualTextDark = Color(0xFF1A2A32)
private val ManualTextGray = Color(0xFF5A6875)

// ── カテゴリ ─────────────────────────────────────────────────────────────

private enum class ManualCategory(val label: String) {
    CHAT    ("チャット"),
    TRIAGE  ("トリアージ"),
    PRIORITY("優先度"),
    MAP     ("地図"),
    OTHER   ("その他"),
}

private fun ManualCategory.icon(): DrawableResource = when (this) {
    ManualCategory.CHAT     -> Res.drawable.ic_chat
    ManualCategory.TRIAGE   -> Res.drawable.ic_triage_selected
    ManualCategory.PRIORITY -> Res.drawable.ic_priority_selected
    ManualCategory.MAP      -> Res.drawable.ic_map_selected
    ManualCategory.OTHER    -> Res.drawable.ic_more_selected
}

// ── ステップデータ ──────────────────────────────────────────────────────

private data class ManualStep(
    val num: String,
    val tag: String,
    val title: String,
    val body: String,
)

private val MANUAL_DATA: Map<ManualCategory, List<ManualStep>> = mapOf(
    ManualCategory.CHAT to listOf(
        ManualStep("1 / 3", "TEXT INPUT",  "テキストで質問する",  "入力欄に症状やケガの状況を入力し、右の「→」ボタンを押して送信します。"),
        ManualStep("2 / 3", "VOICE INPUT", "声で入力する",        "「音声入力」ボタンをタップして録音を開始。話した言葉が自動的にテキストに変換されます。手がふさがっているときに便利です。"),
        ManualStep("3 / 3", "AI RESPONSE", "AIの回答を確認する", "内蔵の知識ベースを参照して、AIが応急処置や対応方法を回答します。"),
    ),
    ManualCategory.TRIAGE to listOf(
        ManualStep("1 / 3", "SAFETY CHECK",  "安全確認",                   "まず自身の安全を確認します。「安全」を選ぶとトリアージを開始します。「危険」を選ぶと地図で避難場所を確認できます。"),
        ManualStep("2 / 3", "TRIAGE FLOW",   "歩行・呼吸・循環・意識の確認", "「はい/いいえ」で4つの質問に答えます。自力で歩ける場合は即座に「軽症」と判定されます。歩けない場合は呼吸・循環・意識を順番に確認します。"),
        ManualStep("3 / 3", "RESULT",        "判定結果の確認",             "「軽症」または「重症」の判定が表示されます。対応の案内文が生成され、結果は自動的に優先度リストに記録されます。"),
    ),
    ManualCategory.PRIORITY to listOf(
        ManualStep("1 / 3", "VICTIM LIST",  "傷病者一覧",     "トリアージ完了後、傷病者が自動的に一覧に記録されます。"),
        ManualStep("2 / 3", "SORT",         "並び替え",       "「時間」または「重症度」ボタンで一覧を並び替えられます。"),
        ManualStep("3 / 3", "EDIT RECORD",  "記録を編集する", "カードの「編集」をタップすると、場所や特徴などのメモを追加できます。"),
    ),
    ManualCategory.MAP to listOf(
        ManualStep("1 / 3", "CURRENT LOCATION", "現在地を確認",   "地図上に現在地がリアルタイムで表示されます。"),
        ManualStep("2 / 3", "SHELTER",           "避難場所を探す", "近くの避難場所や医療施設が地図上にマーカーで表示されます。"),
        ManualStep("3 / 3", "ROUTE",             "ルートを確認",   "避難場所をタップすると、現在地からのルートが表示されます。"),
    ),
    ManualCategory.OTHER to listOf(
        ManualStep("1 / 1", "MANUAL", "マニュアル", "各画面の使い方をカテゴリ別に確認できます。カテゴリを選ぶとステップごとに説明が表示されます。"),
    ),
)

// ── メイン ───────────────────────────────────────────────────────────────

@Composable
fun ManualScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf<ManualCategory?>(null) }
    var currentStep      by remember { mutableStateOf(0) }

    if (selectedCategory == null) {
        CategoryScreen(
            onSelect = { cat ->
                selectedCategory = cat
                currentStep = 0
            },
            onClose = onBack,
            modifier = modifier,
        )
    } else {
        val steps = MANUAL_DATA[selectedCategory!!]!!
        StepScreen(
            category    = selectedCategory!!,
            step        = steps[currentStep],
            totalSteps  = steps.size,
            currentStep = currentStep,
            onNext  = { if (currentStep < steps.lastIndex) currentStep++ else onBack() },
            onPrev  = { if (currentStep > 0) currentStep-- },
            onClose = { selectedCategory = null },
            modifier    = modifier,
        )
    }
}

// ── カテゴリ選択画面 ─────────────────────────────────────────────────────

@Composable
private fun CategoryScreen(
    onSelect: (ManualCategory) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F6))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        // ── ヘッダー ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ManualTeal)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MANUAL",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ManualTeal,
                    letterSpacing = 1.5.sp,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "マニュアル",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = ManualInk,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── カテゴリリスト ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            ManualCategory.entries.forEachIndexed { index, cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(cat) }
                        .padding(horizontal = 30.dp, vertical = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE2F5F7))
                    ) {
                        Icon(
                            painter = painterResource(cat.icon()),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = cat.label,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = ManualInk,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "›",
                        fontSize = 32.sp,
                        color = Color(0xFFCCCCCC),
                    )
                }
                if (index < ManualCategory.entries.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(0.5.dp)
                            .background(Color(0xFFF0F0F0))
                    )
                }
            }
        }
        } // end Column

        // ── 右上 閉じるボタン ─────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 54.dp, end = 34.dp)
                .clickable { onClose() },
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_gray_close),
                contentDescription = "閉じる",
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ── ステップ詳細画面 ─────────────────────────────────────────────────────

@Composable
private fun StepScreen(
    category: ManualCategory,
    step: ManualStep,
    totalSteps: Int,
    currentStep: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ManualDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── スポットライト ─────────────────────────────────────────
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp, vertical = 28.dp),
            ) {
                ManualSpotlight(category = category, stepIndex = currentStep)
            }

            // ── ホワイトカード ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .padding(top = 30.dp, bottom = 28.dp),
                ) {
                    // バッジ行
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 36.dp),
                    ) {
                        Text(
                            text = step.num,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(ManualTeal, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = step.tag,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ManualTeal,
                            letterSpacing = 1.sp,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // プログレスドット
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(totalSteps) { i ->
                            Box(
                                modifier = Modifier
                                    .height(7.dp)
                                    .width(if (i == currentStep) 22.dp else 7.dp)
                                    .background(
                                        if (i == currentStep) ManualTeal else Color(0xFFDDDDDD),
                                        RoundedCornerShape(4.dp),
                                    ),
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // タイトル
                    Text(
                        text = step.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ManualTextDark,
                    )

                    Spacer(Modifier.height(10.dp))

                    // 本文
                    Text(
                        text = step.body,
                        fontSize = 17.sp,
                        color = ManualTextGray,
                        lineHeight = 26.sp,
                    )

                    Spacer(Modifier.height(26.dp))

                    // ナビボタン
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (currentStep > 0) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .border(1.5.dp, Color(0xFFDDDDDD), RoundedCornerShape(50.dp))
                                    .clip(RoundedCornerShape(50.dp))
                                    .clickable { onPrev() }
                                    .padding(horizontal = 18.dp, vertical = 9.dp),
                            ) {
                                Text(
                                    text = "← 戻る",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF8899A5),
                                )
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(ManualTeal, RoundedCornerShape(50.dp))
                                .clip(RoundedCornerShape(50.dp))
                                .clickable { onNext() }
                                .padding(horizontal = 22.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = if (currentStep < totalSteps - 1) "次へ →" else "完了",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }

                // ✕ 閉じるボタン
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 24.dp, end = 24.dp)
                        .size(28.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .clickable { onClose() },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_gray_close),
                        contentDescription = "閉じる",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

// ── スポットライト ────────────────────────────────────────────────────────

@Composable
private fun ManualSpotlight(category: ManualCategory, stepIndex: Int) {
    when (category) {
        ManualCategory.CHAT     -> SpotlightChat(stepIndex)
        ManualCategory.TRIAGE   -> SpotlightTriageFlow(stepIndex)
        ManualCategory.PRIORITY -> SpotlightPriority(stepIndex)
        ManualCategory.MAP      -> SpotlightMap()
        ManualCategory.OTHER    -> SpotlightOther(stepIndex)
    }
}

// ── チャット ─────────────────────────────────────────────────────────────

@Composable
private fun SpotlightChat(stepIndex: Int) {
    when (stepIndex) {
        0 ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF2F4F6), RoundedCornerShape(50.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text("メッセージを入力...", color = Color(0xFFBBBBBB), fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .background(ManualTeal, RoundedCornerShape(50.dp)),
                    ) {
                        Text("→", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        1 ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ManualTeal)
                    .padding(vertical = 24.dp),
            ) {
                Text("音声入力", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        else ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(Color(0xFFE6F8FA), RoundedCornerShape(4.dp, 12.dp, 12.dp, 12.dp))
                            .padding(12.dp),
                    ) {
                        Text(
                            "三角巾を使って手首を固定し、高い位置に保ちましょう。",
                            fontSize = 13.sp,
                            color = ManualTextDark,
                            lineHeight = 19.sp,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("AI", fontSize = 11.sp, color = ManualTeal, modifier = Modifier.padding(start = 4.dp))
                }
            }
    }
}

// ── トリアージ ────────────────────────────────────────────────────────────

@Composable
private fun SpotlightTriageFlow(stepIndex: Int) {
    when (stepIndex) {
        // Step 1: 安全確認
        0 ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("あなた自身は安全ですか？", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ManualTextDark)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f).background(Color(0xFF1D9E75), RoundedCornerShape(50.dp)).padding(vertical = 12.dp),
                        ) { Text("✓  安全", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f).background(Color(0xFFEF9F27), RoundedCornerShape(50.dp)).padding(vertical = 12.dp),
                        ) { Text("⚠  危険", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
            }
        // Step 2: 4ノードのトリアージフロー
        1 -> {
            val nodes = listOf(
                "01" to "歩行確認",
                "02" to "呼吸確認",
                "03" to "循環確認",
                "04" to "意識確認",
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                nodes.forEachIndexed { i, (num, label) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color(0xFFE6F8FA), RoundedCornerShape(6.dp)),
                            ) {
                                Text(num, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ManualTeal)
                            }
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ManualTextDark)
                        }
                    }
                    if (i < nodes.lastIndex) {
                        Text("↓", fontSize = 14.sp, color = Color(0xFF778899), modifier = Modifier.padding(vertical = 3.dp))
                    }
                }
            }
        }
        // Step 3: 軽症 / 重症 のみ
        else ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        Triple(Color(0xFF1D9E75), "軽症", "救護所へ自力で"),
                        Triple(Color(0xFFE24B4A), "重症", "緊急処置が必要"),
                    ).forEach { (color, jp, desc) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(color, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(jp, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(desc, fontSize = 13.sp, color = ManualTextGray)
                        }
                    }
                }
            }
    }
}

// ── 優先度 ────────────────────────────────────────────────────────────────

@Composable
private fun SpotlightPriority(stepIndex: Int) {
    when (stepIndex) {
        0 ->
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple(Color(0xFFE5463F), "#001", "重症"),
                    Triple(Color(0xFF16A36B), "#002", "軽傷"),
                ).forEach { (color, num, label) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(3.dp).height(32.dp).background(color))
                            Spacer(Modifier.width(10.dp))
                            Text(num, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .background(color, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        1 ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Column {
                    Text("Sort By", fontSize = 11.sp, color = ManualTextGray, letterSpacing = 0.6.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(ManualTeal, RoundedCornerShape(50.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) { Text("時間", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(50.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) { Text("重症度", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ManualTextGray) }
                    }
                }
            }
        else ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("EDIT RECORD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ManualTeal, letterSpacing = 1.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF2F4F6), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(Res.drawable.ic_place), null, tint = Color.Unspecified, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("場所を入力...", fontSize = 13.sp, color = Color(0xFFBBBBBB))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF2F4F6), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(Res.drawable.ic_tag), null, tint = Color.Unspecified, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("特徴を入力...", fontSize = 13.sp, color = Color(0xFFBBBBBB))
                    }
                }
            }
    }
}

// ── 地図 ──────────────────────────────────────────────────────────────────

@Composable
private fun SpotlightMap() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFCFDFBF))
            .padding(24.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            listOf(
                ManualTeal        to "現在地",
                Color(0xFFE5463F) to "避難場所",
                Color(0xFFEF9F27) to "医療施設",
            ).forEach { (color, label) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(color, RoundedCornerShape(50.dp))
                            .border(2.5.dp, Color.White, RoundedCornerShape(50.dp))
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ManualTextDark)
                }
            }
        }
    }
}

// ── その他 ────────────────────────────────────────────────────────────────

@Composable
private fun SpotlightOther(stepIndex: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE2F5F7))
            ) {
                Icon(
                    painter = painterResource(if (stepIndex == 0) Res.drawable.ic_manual else Res.drawable.ic_settings),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = if (stepIndex == 0) "マニュアル" else "設定",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = ManualInk,
                modifier = Modifier.weight(1f),
            )
            Text("›", fontSize = 22.sp, color = Color(0xFFCCCCCC))
        }
    }
}

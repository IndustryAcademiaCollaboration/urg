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
import androidx.compose.foundation.layout.statusBarsPadding
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
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_gray_close

private val Teal     = Color(0xFF25B1BF)
private val DarkBg   = Color(0xFF141719)
private val TextDark = Color(0xFF1A2A32)
private val TextGray = Color(0xFF5A6875)

private data class ManualStep(
    val num: String,
    val tag: String,
    val title: String,
    val body: String,
)

private val MANUAL_STEPS = listOf(
    ManualStep(
        num = "1 / 4",
        tag = "TEXT INPUT",
        title = "テキストで状況を伝える",
        body = "入力欄に症状やケガの状況を入力し、右の「→」ボタンを押して送信します。",
    ),
    ManualStep(
        num = "2 / 4",
        tag = "VOICE INPUT",
        title = "声で状況を伝える",
        body = "「音声入力」ボタンを押すと、話した言葉がそのまま文字になります。手がふさがっている時に便利です。",
    ),
    ManualStep(
        num = "3 / 4",
        tag = "SAFETY CHECK",
        title = "安全確認について",
        body = "トリアージ開始前に安全確認があります。\n\n安全な場所にいる場合 → トリアージを開始します\n危険な場所にいる場合 → 避難マップに切り替わります",
    ),
    ManualStep(
        num = "4 / 4",
        tag = "START TRIAGE",
        title = "トリアージを開始する",
        body = "トリアージ画面でチェックを進め、緊急度を判定します。対応の優先順位が表示されます。",
    ),
)

@Composable
fun ManualScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableStateOf(0) }
    val step = MANUAL_STEPS[currentStep]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── スポットライトイラスト ────────────────────────────────────
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp, vertical = 28.dp),
            ) {
                when (currentStep) {
                    0 -> SpotlightTextInput()
                    1 -> SpotlightVoiceInput()
                    2 -> SpotlightSafetyCheck()
                    else -> SpotlightTriage()
                }
            }

            // ── ホワイトカード ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White),
            ) {
                // メインコンテンツ
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .padding(top = 22.dp, bottom = 28.dp),
                ) {
                    // バッジ行（右側に閉じるボタン分の余白）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 36.dp),
                    ) {
                        Text(
                            text = step.num,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(Teal, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = step.tag,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Teal,
                            letterSpacing = 0.8.sp,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // プログレスドット
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(MANUAL_STEPS.size) { i ->
                            Box(
                                modifier = Modifier
                                    .height(7.dp)
                                    .width(if (i == currentStep) 22.dp else 7.dp)
                                    .background(
                                        if (i == currentStep) Teal else Color(0xFFDDDDDD),
                                        RoundedCornerShape(4.dp),
                                    ),
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // タイトル
                    Text(
                        text = step.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                    )

                    Spacer(Modifier.height(8.dp))

                    // 本文
                    Text(
                        text = step.body,
                        fontSize = 15.sp,
                        color = TextGray,
                        lineHeight = 23.sp,
                    )

                    Spacer(Modifier.height(22.dp))

                    // ナビボタン行
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
                                    .clickable { currentStep-- }
                                    .padding(horizontal = 18.dp, vertical = 9.dp),
                            ) {
                                Text(
                                    text = "← 戻る",
                                    fontSize = 13.sp,
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
                                .background(Teal, RoundedCornerShape(50.dp))
                                .clip(RoundedCornerShape(50.dp))
                                .clickable {
                                    if (currentStep < MANUAL_STEPS.lastIndex) currentStep++
                                    else onBack()
                                }
                                .padding(horizontal = 22.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = if (currentStep < MANUAL_STEPS.lastIndex) "次へ →" else "完了",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }

                // ✕ 閉じるボタン（カード右上角）
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 14.dp, end = 14.dp)
                        .size(28.dp)
                        .background(Color(0xFFF0F0F0), RoundedCornerShape(50.dp))
                        .clip(RoundedCornerShape(50.dp))
                        .clickable { onBack() },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_gray_close),
                        contentDescription = "閉じる",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

// ── スポットライトイラスト ────────────────────────────────────────────────────

@Composable
private fun SpotlightTextInput() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFF2F4F6), RoundedCornerShape(50.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "メッセージを入力...",
                    color = Color(0xFFBBBBBB),
                    fontSize = 14.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .background(Teal, RoundedCornerShape(50.dp)),
            ) {
                Text("→", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpotlightVoiceInput() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(Teal, RoundedCornerShape(16.dp))
            .padding(vertical = 22.dp),
    ) {
        Text(
            text = "音声入力",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SpotlightSafetyCheck() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "あなた自身は安全ですか？",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFE8FAF3), RoundedCornerShape(50.dp))
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = "✓  安全",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E9E6E),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFFCEBEB), RoundedCornerShape(50.dp))
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = "⚠  危険",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE24B4A),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotlightTriage() {
    val nodes = listOf(
        Triple("①", "歩行確認", "自力で歩けますか？"),
        Triple("②", "呼吸確認", "呼吸はありますか？"),
        Triple("③", "循環確認", "脈はありますか？"),
        Triple("④", "意識確認", "呼びかけに反応しますか？"),
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        nodes.forEachIndexed { i, (num, label, question) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color(0xFFE6F8FA), RoundedCornerShape(50.dp)),
                    ) {
                        Text(
                            text = num,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Teal,
                        )
                    }
                    Column {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                        )
                        Text(
                            text = question,
                            fontSize = 10.sp,
                            color = TextGray,
                        )
                    }
                }
            }
            if (i < nodes.lastIndex) {
                Text(
                    text = "↓",
                    fontSize = 14.sp,
                    color = Color(0xFF778899),
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

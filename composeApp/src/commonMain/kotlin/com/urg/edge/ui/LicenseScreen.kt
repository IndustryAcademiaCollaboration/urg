package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_gray_close

private val LicenseTeal     = Color(0xFF25B1BF)
private val LicenseInk      = Color(0xFF10202A)
private val LicenseTextDark = Color(0xFF1A2A32)
private val LicenseTextGray = Color(0xFF5A6875)

// ── 表示データ ───────────────────────────────────────────────────────────
// 各ライセンスが要求する著作権表示・帰属表示をここに集約する。
// 素材やライブラリを追加・変更した際は、必ずこのリストも更新すること。

private data class LicenseEntry(
    val name: String,
    val body: String,
    val url: String? = null,
)

private data class LicenseSection(
    val title: String,
    val entries: List<LicenseEntry>,
)

private val licenseSections = listOf(
    LicenseSection(
        title = "音声合成",
        entries = listOf(
            LicenseEntry(
                name = "つくよみちゃんコーパス（CV.夢前黎）",
                // つくよみちゃんコーパスの利用規約が定める表記。文言を勝手に変えないこと。
                body = "本ソフトウェアの音声合成には、フリー素材キャラクター" +
                    "「つくよみちゃん」が無料公開している音声データを使用しています。",
                url = "https://tyc.rei-yumesaki.net/material/corpus/",
            ),
        ),
    ),
    LicenseSection(
        title = "地図データ",
        entries = listOf(
            LicenseEntry(
                name = "OpenStreetMap",
                body = "本アプリの地図および経路データは、OpenStreetMap の貢献者が作成し、" +
                    "Geofabrik GmbH が加工・提供したデータを使用しています。\n" +
                    "© OpenStreetMap contributors / Open Database License (ODbL) 1.0",
                url = "https://opendatacommons.org/licenses/odbl/",
            ),
            LicenseEntry(
                name = "tilemaker",
                body = "地図データのベクタタイル変換に使用しています。",
            ),
        ),
    ),
    LicenseSection(
        title = "公的データ",
        entries = listOf(
            LicenseEntry(
                name = "気象庁",
                body = "地震情報の取得に気象庁ホームページのデータを使用しています。\n" +
                    "出典：気象庁ホームページ",
                url = "https://www.jma.go.jp/bosai/quake/data/list.json",
            ),
            LicenseEntry(
                name = "避難所・医療機関の位置情報",
                body = "指定緊急避難場所・指定避難所・救護所・病院の位置情報は、" +
                    "国および自治体が公開するオープンデータを加工して作成しています。\n" +
                    "各提供元が定める利用条件に従って使用しています。\n" +
                    "本データは当方が加工したものであり、" +
                    "その内容について各提供元が保証するものではありません。",
            ),
        ),
    ),
    LicenseSection(
        title = "AI モデル",
        entries = listOf(
            LicenseEntry(
                name = "Gemma",
                body = "Gemma is provided under and subject to the Gemma Terms of Use.",
                url = "https://ai.google.dev/gemma/terms",
            ),
            LicenseEntry(
                name = "multilingual-e5-small",
                body = "intfloat / MIT License",
            ),
            LicenseEntry(
                name = "ReazonSpeech k2 v2",
                body = "Reazon Human Interaction Lab / Apache License 2.0",
            ),
        ),
    ),
    LicenseSection(
        title = "オープンソースライブラリ",
        entries = listOf(
            LicenseEntry(
                name = "Apache License 2.0",
                body = "LiteRT-LM (Google)\n" +
                    "sherpa-onnx (k2-fsa)\n" +
                    "Deep Java Library (Amazon)\n" +
                    "SQLDelight (Cash App)\n" +
                    "AndroidX / Jetpack Compose (Google)\n" +
                    "Kotlin / Compose Multiplatform (JetBrains)",
            ),
            LicenseEntry(
                name = "MIT License",
                body = "ONNX Runtime (Microsoft)\n" +
                    "piper-plus (ayutaz)\n" +
                    "BRouter (abrensch)",
            ),
            LicenseEntry(
                name = "BSD 2-Clause License",
                body = "MapLibre GL Native (MapLibre)",
            ),
        ),
    ),
)

// ── 画面 ─────────────────────────────────────────────────────────────────

@Composable
fun LicenseScreen(
    onBack: () -> Unit,
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomPadding)
        ) {
            Header()

            Spacer(modifier = Modifier.height(20.dp))

            licenseSections.forEach { section ->
                SectionCard(section)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "本アプリは上記の著作物を、各提供元が定める利用条件に従って使用しています。",
                fontSize = 12.sp,
                color = LicenseTextGray,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── 閉じるボタン ────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 20.dp)
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .clickable { onBack() },
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

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LicenseTeal)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "LICENSE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = LicenseTeal,
                letterSpacing = 1.5.sp,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "ライセンス",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = LicenseInk,
        )
    }
}

@Composable
private fun SectionCard(section: LicenseSection) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = section.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = LicenseTeal,
        )

        section.entries.forEachIndexed { index, entry ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE8ECEF))
            }
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = entry.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = LicenseTextDark,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry.body,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = LicenseTextGray,
            )
            entry.url?.let { url ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = url,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = LicenseTeal,
                )
            }
        }
    }
}

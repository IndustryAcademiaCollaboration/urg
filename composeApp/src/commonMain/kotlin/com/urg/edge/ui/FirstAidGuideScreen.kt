package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urg.edge.KnowledgeChunk

private sealed interface GuideScreen {
    object CategoryList : GuideScreen
    data class SubcategoryList(val category: String) : GuideScreen
    data class ChunkList(val category: String, val subcategory: String) : GuideScreen
    data class ChunkDetail(val chunk: KnowledgeChunk) : GuideScreen
}

private val GuideTeal   = Color(0xFF25B1BF)
private val GuideRed    = Color(0xFFD94444)
private val GuideBg     = Color(0xFFF2F4F6)
private val GuideInk    = Color(0xFF10202A)
private val GuideMuted  = Color(0xFF788E98)

@Composable
fun FirstAidGuideScreen(
    chunks: List<KnowledgeChunk>,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    var screen by remember { mutableStateOf<GuideScreen>(GuideScreen.CategoryList) }

    Column(modifier = modifier.fillMaxSize().background(GuideBg)) {

        // ── ヘッダー ──────────────────────────────────────────────────
        GuideHeader(
            screen = screen,
            onBack = {
                screen = when (val s = screen) {
                    is GuideScreen.CategoryList    -> s
                    is GuideScreen.SubcategoryList -> GuideScreen.CategoryList
                    is GuideScreen.ChunkList       -> GuideScreen.SubcategoryList(s.category)
                    is GuideScreen.ChunkDetail     -> GuideScreen.ChunkList(s.chunk.category, s.chunk.subcategory)
                }
            }
        )

        // ── コンテンツ ────────────────────────────────────────────────
        when (val s = screen) {

            is GuideScreen.CategoryList -> {
                val categories = remember(chunks) { chunks.map { it.category }.distinct() }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(categories) { category ->
                        val count = chunks.count { it.category == category }
                        val hasSevere = chunks.any { it.category == category && it.severity == "重症" }
                        GuideListCard(
                            title    = category,
                            subtitle = "${count}項目",
                            severe   = hasSevere,
                            onClick  = { screen = GuideScreen.SubcategoryList(category) }
                        )
                    }
                }
            }

            is GuideScreen.SubcategoryList -> {
                val subcategories = remember(chunks, s.category) {
                    chunks.filter { it.category == s.category }.map { it.subcategory }.distinct()
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(subcategories) { sub ->
                        val count = chunks.count { it.category == s.category && it.subcategory == sub }
                        val hasSevere = chunks.any { it.category == s.category && it.subcategory == sub && it.severity == "重症" }
                        GuideListCard(
                            title    = sub,
                            subtitle = "${count}項目",
                            severe   = hasSevere,
                            onClick  = { screen = GuideScreen.ChunkList(s.category, sub) }
                        )
                    }
                }
            }

            is GuideScreen.ChunkList -> {
                val filtered = remember(chunks, s.category, s.subcategory) {
                    chunks.filter { it.category == s.category && it.subcategory == s.subcategory }
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered) { chunk ->
                        GuideListCard(
                            title    = chunk.title,
                            subtitle = chunk.whenToUse,
                            severe   = chunk.severity == "重症",
                            showBadge = chunk.severity == "重症",
                            onClick  = { screen = GuideScreen.ChunkDetail(chunk) }
                        )
                    }
                }
            }

            is GuideScreen.ChunkDetail -> {
                ChunkDetailView(chunk = s.chunk, bottomPadding = bottomPadding)
            }
        }
    }
}

// ── ヘッダー ─────────────────────────────────────────────────────────

@Composable
private fun GuideHeader(screen: GuideScreen, onBack: () -> Unit) {
    val isRoot = screen is GuideScreen.CategoryList

    // ルートの場合は「辞書」シンプルタイトル
    if (isRoot) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "辞書",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = GuideInk,
            )
        }
        HorizontalDivider(color = Color(0xFFEDF0F2), thickness = 1.dp)
        return
    }

    // 非ルート：戻る + タイトル + パンくず
    val title = when (screen) {
        is GuideScreen.SubcategoryList -> screen.category
        is GuideScreen.ChunkList       -> screen.subcategory
        is GuideScreen.ChunkDetail     -> screen.chunk.title
        else                           -> ""
    }
    val breadcrumb = when (screen) {
        is GuideScreen.ChunkList   -> screen.category
        is GuideScreen.ChunkDetail -> "${screen.chunk.category}  ›  ${screen.chunk.subcategory}"
        else                       -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // 戻る行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "‹ 戻る",
                fontSize = 15.sp,
                color = GuideTeal,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = GuideInk,
                maxLines = 1,
            )
        }

        // パンくず
        if (breadcrumb != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF6F8FA))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(GuideTeal, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = breadcrumb, fontSize = 12.sp, color = GuideMuted)
            }
        }

        HorizontalDivider(color = Color(0xFFEDF0F2), thickness = 1.dp)
    }
}

// ── カード ────────────────────────────────────────────────────────────

@Composable
private fun GuideListCard(
    title: String,
    subtitle: String,
    severe: Boolean = false,
    showBadge: Boolean = false,
    onClick: () -> Unit,
) {
    val accentColor = if (severe) GuideRed else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEDF0F2), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 左アクセントバー（重症のみ赤）
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(IntrinsicSize.Min)
                    .background(accentColor)
                    .defaultMinSize(minHeight = 56.dp)
            )

            // コンテンツ
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GuideInk,
                        )
                        if (showBadge) {
                            Spacer(modifier = Modifier.width(8.dp))
                            SeverityBadge()
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = GuideMuted,
                        maxLines = 2,
                        lineHeight = 17.sp,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "›", fontSize = 20.sp, color = GuideTeal)
            }
        }
    }
}

@Composable
private fun SeverityBadge() {
    Text(
        text = "重症",
        fontSize = 11.sp,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(GuideRed, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// ── 詳細ビュー ────────────────────────────────────────────────────────

@Composable
private fun ChunkDetailView(
    chunk: KnowledgeChunk,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 重症バッジ
        if (chunk.severity == "重症") {
            Row {
                SeverityBadge()
            }
        }

        // こんなとき
        DetailCard(title = "こんなとき", titleColor = GuideTeal) {
            Text(text = chunk.whenToUse, fontSize = 14.sp, color = GuideInk, lineHeight = 20.sp)
        }

        // やること
        DetailCard(title = "やること", titleColor = GuideTeal) {
            chunk.steps.forEachIndexed { i, step ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .background(GuideTeal, RoundedCornerShape(50))
                    ) {
                        Text(
                            text = "${i + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = step, fontSize = 14.sp, color = GuideInk, lineHeight = 20.sp,
                        modifier = Modifier.weight(1f))
                }
            }
        }

        // やってはいけないこと
        if (chunk.doNot.isNotEmpty()) {
            DetailCard(title = "やってはいけないこと", titleColor = GuideRed) {
                chunk.doNot.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 13.sp,
                            color = GuideRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp),
                        )
                        Text(text = item, fontSize = 14.sp, color = GuideInk, lineHeight = 20.sp,
                            modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    titleColor: Color = GuideTeal,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEDF0F2), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .background(titleColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Column(content = content)
    }
}

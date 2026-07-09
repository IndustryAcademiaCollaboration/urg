package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urg.edge.KnowledgeChunk
import kotlin.math.roundToInt

// ── 定数 ─────────────────────────────────────────────────────────────

private val GuideTeal  = Color(0xFF25B1BF)
private val GuideRed   = Color(0xFFD94444)
private val GuideBg    = Color(0xFFF2F4F6)
private val GuideInk   = Color(0xFF10202A)
private val GuideMuted = Color(0xFF788E98)

private val disasterPhases = listOf(
    "平常時",
    "災害発生直後",
    "災害後数時間",
    "災害後数日",
    "災害後1週間",
    "災害後長期",
)

// ── 画面状態 ──────────────────────────────────────────────────────────

private sealed interface GuideScreen {
    object CategoryList : GuideScreen
    data class SubcategoryList(val category: String) : GuideScreen
    data class ChunkList(val category: String, val subcategory: String) : GuideScreen
    data class ChunkDetail(val chunk: KnowledgeChunk) : GuideScreen
}

// ── メイン Composable ────────────────────────────────────────────────

@Composable
fun FirstAidGuideScreen(
    chunks: List<KnowledgeChunk>,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    var screen by remember { mutableStateOf<GuideScreen>(GuideScreen.CategoryList) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("すべて") }
    var selectedPhaseIndex by remember { mutableStateOf(0) }

    val selectedPhase = disasterPhases[selectedPhaseIndex]

    val filterOptions = remember(chunks) {
        listOf("すべて") + chunks.map { it.subcategory }.distinct()
    }

    val filteredChunks = remember(chunks, searchQuery, selectedFilter, selectedPhase) {
        chunks.filter { chunk ->
            chunk.matchesGuideSearch(searchQuery) &&
                chunk.matchesGuideFilter(selectedFilter) &&
                chunk.matchesPhase(selectedPhase)
        }
    }

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

        // ── 検索・フィルター（詳細画面以外） ─────────────────────────
        if (screen !is GuideScreen.ChunkDetail) {
            GuideSearchAndFilterBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    selectedFilter = "すべて"
                    screen = GuideScreen.CategoryList
                },
                selectedFilter = selectedFilter,
                filterOptions = filterOptions,
                onFilterChange = {
                    selectedFilter = it
                    screen = GuideScreen.CategoryList
                },
                selectedPhaseIndex = selectedPhaseIndex,
                onPhaseChange = {
                    selectedPhaseIndex = it
                    screen = GuideScreen.CategoryList
                }
            )
        }

        // ── コンテンツ ────────────────────────────────────────────────
        when (val s = screen) {

            is GuideScreen.CategoryList -> {
                val categories = remember(filteredChunks) {
                    filteredChunks.map { it.category }.distinct()
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(categories) { category ->
                        val count = filteredChunks.count { it.category == category }
                        val hasSevere = filteredChunks.any { it.category == category && it.severity == "重症" }
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
                val subcategories = remember(filteredChunks, s.category) {
                    filteredChunks.filter { it.category == s.category }.map { it.subcategory }.distinct()
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(subcategories) { sub ->
                        val count = filteredChunks.count { it.category == s.category && it.subcategory == sub }
                        val hasSevere = filteredChunks.any { it.category == s.category && it.subcategory == sub && it.severity == "重症" }
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
                val filtered = remember(filteredChunks, s.category, s.subcategory) {
                    filteredChunks.filter { it.category == s.category && it.subcategory == s.subcategory }
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered) { chunk ->
                        GuideListCard(
                            title     = chunk.title,
                            subtitle  = chunk.whenToUse,
                            severe    = chunk.severity == "重症",
                            showBadge = chunk.severity == "重症",
                            onClick   = { screen = GuideScreen.ChunkDetail(chunk) }
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

    if (isRoot) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(text = "辞書", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GuideInk)
        }
        HorizontalDivider(color = Color(0xFFEDF0F2), thickness = 1.dp)
        return
    }

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

    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
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
            Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GuideInk, maxLines = 1)
        }

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

// ── 検索・フィルターバー ──────────────────────────────────────────────

@Composable
private fun GuideSearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    filterOptions: List<String>,
    onFilterChange: (String) -> Unit,
    selectedPhaseIndex: Int,
    onPhaseChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GuideSearchInput(value = query, onValueChange = onQueryChange)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            filterOptions.forEach { option ->
                FilterChip(
                    selected = selectedFilter == option,
                    onClick  = { onFilterChange(option) },
                    label    = { Text(text = option, fontSize = 12.sp) }
                )
            }
        }

        GuidePhaseSlider(selectedPhaseIndex = selectedPhaseIndex, onPhaseChange = onPhaseChange)
    }
    HorizontalDivider(color = Color(0xFFEDF0F2), thickness = 1.dp)
}

@Composable
private fun GuideSearchInput(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GuideBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(text = "応急手当を検索...", fontSize = 14.sp, color = Color(0xFF999999))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = GuideInk),
            cursorBrush = SolidColor(GuideTeal),
            modifier = Modifier.fillMaxWidth().padding(end = 32.dp)
        )
        if (value.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFDDE3E8))
                    .clickable { onValueChange("") }
            ) {
                Text("×", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF666666))
            }
        }
    }
}

@Composable
private fun GuidePhaseSlider(selectedPhaseIndex: Int, onPhaseChange: (Int) -> Unit) {
    val safeIndex = selectedPhaseIndex.coerceIn(0, disasterPhases.lastIndex)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = disasterPhases[safeIndex], fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GuideTeal)
        Slider(
            value = safeIndex.toFloat(),
            onValueChange = { onPhaseChange(it.roundToInt().coerceIn(0, disasterPhases.lastIndex)) },
            valueRange = 0f..disasterPhases.lastIndex.toFloat(),
            steps = disasterPhases.size - 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            disasterPhases.forEach { phase ->
                Text(text = phase, fontSize = 8.sp, color = GuideMuted, maxLines = 2,
                    modifier = Modifier.width(48.dp))
            }
        }
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
                    .defaultMinSize(minHeight = 60.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = GuideInk)
                        if (showBadge) {
                            Spacer(modifier = Modifier.width(8.dp))
                            SeverityBadge()
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 12.sp, color = GuideMuted, maxLines = 2, lineHeight = 17.sp)
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
private fun ChunkDetailView(chunk: KnowledgeChunk, bottomPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (chunk.severity == "重症") {
            Row { SeverityBadge() }
        }

        DetailCard(title = "こんなとき", titleColor = GuideTeal) {
            Text(text = chunk.whenToUse, fontSize = 14.sp, color = GuideInk, lineHeight = 20.sp)
        }

        DetailCard(title = "やること", titleColor = GuideTeal) {
            chunk.steps.forEachIndexed { i, step ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(22.dp).background(GuideTeal, RoundedCornerShape(50))
                    ) {
                        Text("${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = step, fontSize = 14.sp, color = GuideInk, lineHeight = 20.sp,
                        modifier = Modifier.weight(1f))
                }
            }
        }

        if (chunk.doNot.isNotEmpty()) {
            DetailCard(title = "やってはいけないこと", titleColor = GuideRed) {
                chunk.doNot.forEach { item ->
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                        Text("✕", fontSize = 13.sp, color = GuideRed, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp))
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
            Box(modifier = Modifier.width(3.dp).height(14.dp).background(titleColor, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = titleColor)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Column(content = content)
    }
}

// ── 検索・フィルター ロジック ─────────────────────────────────────────

private fun KnowledgeChunk.matchesGuideSearch(query: String): Boolean {
    val keywords = expandGuideSearchQuery(query)
    if (keywords.isEmpty()) return true
    val targetText = "$title $category $subcategory $whenToUse ${severity.orEmpty()} ${steps.joinToString(" ")} ${doNot.joinToString(" ")}".lowercase()
    return keywords.any { targetText.contains(it.lowercase()) }
}

private fun KnowledgeChunk.matchesGuideFilter(filter: String): Boolean = when (filter) {
    "すべて" -> true
    "重症"   -> severity == "重症"
    else     -> category == filter || subcategory == filter
}

private fun KnowledgeChunk.matchesPhase(phaseTag: String): Boolean {
    if (tags.isEmpty()) return true
    return tags.contains(phaseTag)
}

private fun expandGuideSearchQuery(query: String): List<String> {
    val text = query.trim()
    if (text.isBlank()) return emptyList()
    val keywords = mutableListOf(text)
    if (text.contains("水") && (text.contains("出ない") || text.contains("でない"))) keywords += "断水"
    if (text.contains("電気") || text.contains("明かり") || text.contains("あかり") || text.contains("停電")) keywords += "停電"
    if (text.contains("充電") || text.contains("バッテリー") || text.contains("スマホ") || text.contains("携帯")) {
        keywords += "電池"; keywords += "充電"
    }
    return keywords.distinct()
}

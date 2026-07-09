package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urg.edge.KnowledgeChunk
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_back
import urg.composeapp.generated.resources.ic_bandage
import urg.composeapp.generated.resources.ic_earthquake
import urg.composeapp.generated.resources.ic_guide
import urg.composeapp.generated.resources.ic_heart
import urg.composeapp.generated.resources.ic_inpatient
import urg.composeapp.generated.resources.ic_pill
import urg.composeapp.generated.resources.ic_search
import urg.composeapp.generated.resources.ic_sling
import urg.composeapp.generated.resources.ic_stress
import urg.composeapp.generated.resources.ic_support
import urg.composeapp.generated.resources.ic_symptoms

// ── 定数 ─────────────────────────────────────────────────────────────

private val GuideTeal  = Color(0xFF25B1BF)
private val GuideRed   = Color(0xFFD94444)
private val GuideBg    = Color(0xFFF5F6F8)
private val GuideInk   = Color(0xFF10202A)
private val GuideMuted = Color(0xFFA0AEB6)

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
    bottomPadding: Dp = 0.dp,
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

        // ── 検索・フィルター（カテゴリ一覧のみ） ─────────────────────
        if (screen is GuideScreen.CategoryList) {
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

        // ── コンテンツ（weight(1f) で残り高さを確定させる） ──────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when (val s = screen) {

            is GuideScreen.CategoryList -> {
                val categories = remember(filteredChunks) {
                    filteredChunks.map { it.category }.distinct()
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        top = 14.dp,
                        end = 14.dp,
                        bottom = 14.dp + bottomPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    gridItems(categories) { category ->
                        val count = filteredChunks.count { it.category == category }
                        GuideCategoryCard(
                            title   = category,
                            count   = count,
                            onClick = { screen = GuideScreen.SubcategoryList(category) }
                        )
                    }
                }
            }

            is GuideScreen.SubcategoryList -> {
                val subcategories = remember(filteredChunks, s.category) {
                    filteredChunks.filter { it.category == s.category }
                        .map { it.subcategory }.distinct()
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 14.dp, top = 14.dp, end = 14.dp,
                        bottom = 14.dp + bottomPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(subcategories) { sub ->
                        val count = filteredChunks.count {
                            it.category == s.category && it.subcategory == sub
                        }
                        val hasSevere = filteredChunks.any {
                            it.category == s.category && it.subcategory == sub && it.severity == "重症"
                        }
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
                    filteredChunks.filter {
                        it.category == s.category && it.subcategory == s.subcategory
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 14.dp, top = 14.dp, end = 14.dp,
                        bottom = 14.dp + bottomPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
        } // Box weight(1f)
    }
}

// ── ヘッダー ─────────────────────────────────────────────────────────

@Composable
private fun GuideHeader(screen: GuideScreen, onBack: () -> Unit) {
    val isRoot = screen is GuideScreen.CategoryList

    if (isRoot) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GuideTeal)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FIRST AID GUIDE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GuideTeal,
                    letterSpacing = 1.5.sp,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "応急手当ガイド",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = GuideInk,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        return
    }

    val title = when (screen) {
        is GuideScreen.SubcategoryList -> screen.category
        is GuideScreen.ChunkList       -> screen.subcategory
        is GuideScreen.ChunkDetail     -> screen.chunk.title
        else                           -> ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GuideBg)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_back),
            contentDescription = "戻る",
            tint = GuideTeal,
            modifier = Modifier
                .size(22.dp)
                .clickable { onBack() },
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = GuideInk,
            maxLines = 1,
        )
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
    Column(modifier = Modifier.fillMaxWidth()) {
        // 検索バー（白いカード）
        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp)
        ) {
            GuideSearchInput(value = query, onValueChange = onQueryChange)
        }

        // フィルターチップ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            filterOptions.forEach { option ->
                val selected = selectedFilter == option
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) GuideTeal else Color.White)
                        .border(
                            width = 1.5.dp,
                            color = if (selected) GuideTeal else Color(0xFFE2E8ED),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .clickable { onFilterChange(option) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = option,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color.White else GuideMuted,
                    )
                }
            }
        }

        // フェーズスライダー（白いカードで包む）
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            GuidePhaseSlider(selectedPhaseIndex = selectedPhaseIndex, onPhaseChange = onPhaseChange)
        }
    }
}

@Composable
private fun GuideSearchInput(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F2F4), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                painter = painterResource(Res.drawable.ic_search),
                contentDescription = null,
                tint = GuideMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(text = "症状・キーワードで探す", fontSize = 14.sp, color = GuideMuted)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = GuideInk),
                    cursorBrush = SolidColor(GuideTeal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (value.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFDDE3E8))
                        .clickable { onValueChange("") }
                ) {
                    Text("×", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF666666))
                }
            }
        }
    }
}

@Composable
private fun GuidePhaseSlider(selectedPhaseIndex: Int, onPhaseChange: (Int) -> Unit) {
    val safeIndex = selectedPhaseIndex.coerceIn(0, disasterPhases.lastIndex)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 4.dp),
    ) {
        Text(
            text = disasterPhases[safeIndex],
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GuideTeal,
        )
        // ── スライダー＋ドットオーバーレイ ───────────────────
        Box(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = safeIndex.toFloat(),
                onValueChange = { onPhaseChange(it.roundToInt().coerceIn(0, disasterPhases.lastIndex)) },
                valueRange = 0f..disasterPhases.lastIndex.toFloat(),
                steps = disasterPhases.size - 2,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = GuideTeal,
                    activeTrackColor = GuideTeal,
                    inactiveTrackColor = Color(0xFFE2E8ED),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
            // Slider トラック上にドットをオーバーレイ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                disasterPhases.forEachIndexed { i, _ ->
                    val isCurrent = i == safeIndex
                    val isActive  = i < safeIndex
                    if (isCurrent) {
                        // 現在位置はサムが表示されるのでスペースのみ
                        Spacer(modifier = Modifier.size(10.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (isActive) Color.White.copy(alpha = 0.85f)
                                            else Color(0xFFBBC8CF),
                                    shape = RoundedCornerShape(50),
                                )
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        // ── フェーズラベル ────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            disasterPhases.forEachIndexed { i, phase ->
                Text(
                    text = phase,
                    fontSize = 8.sp,
                    color = if (i == safeIndex) GuideTeal else GuideMuted,
                    fontWeight = if (i == safeIndex) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    modifier = Modifier.width(42.dp),
                )
            }
        }
    }
}

// ── カテゴリグリッドカード ────────────────────────────────────────────

@Composable
private fun GuideCategoryCard(
    title: String,
    count: Int,
    onClick: () -> Unit,
) {
    val iconRes = categoryIconRes(title)
    val bgColor = categoryIconBg(title)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFECEFF2), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(bgColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = title,
                tint = GuideTeal,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = GuideInk,
            lineHeight = 19.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "${count}項目", fontSize = 11.sp, color = GuideMuted)
    }
}


private fun categoryIconRes(category: String): DrawableResource = when {
    category.contains("心") || category.contains("呼吸") || category.contains("CPR") -> Res.drawable.ic_heart
    category.contains("持病") || category.contains("慢性")                            -> Res.drawable.ic_pill
    category.contains("体調") || category.contains("不良")                            -> Res.drawable.ic_inpatient
    category.contains("症状")                                                          -> Res.drawable.ic_symptoms
    category.contains("災害") || category.contains("救助")                            -> Res.drawable.ic_earthquake
    category.contains("外傷") || (category.contains("けが") && category.contains("外")) -> Res.drawable.ic_sling
    category.contains("けが") || category.contains("病気")                            -> Res.drawable.ic_bandage
    category.contains("精神") || category.contains("サポート") || category.contains("ケア") || category.contains("メンタル") -> Res.drawable.ic_support
    category.contains("要配慮") || category.contains("ストレス")                      -> Res.drawable.ic_stress
    else                                                                               -> Res.drawable.ic_guide
}

private fun categoryIconBg(@Suppress("UNUSED_PARAMETER") category: String): Color = Color(0xFFE2F5F7)

// ── リストカード（サブカテゴリ・手技リスト用）────────────────────────

@Composable
private fun GuideListCard(
    title: String,
    subtitle: String,
    severe: Boolean = false,
    showBadge: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFECEFF2), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = GuideInk,
                )
                if (showBadge) {
                    Spacer(modifier = Modifier.width(8.dp))
                    SeverityBadge()
                }
            }
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = GuideMuted,
                    maxLines = 2,
                    lineHeight = 18.sp,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "›", fontSize = 22.sp, color = Color(0xFFC8D0D6))
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
            .background(GuideRed, RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// ── 詳細ビュー ────────────────────────────────────────────────────────

@Composable
private fun ChunkDetailView(chunk: KnowledgeChunk, bottomPadding: Dp = 0.dp) {
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
            Text(text = chunk.whenToUse, fontSize = 14.sp, color = GuideInk, lineHeight = 22.sp)
        }

        DetailCard(title = "やること", titleColor = GuideTeal) {
            chunk.steps.forEachIndexed { i, step ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .background(GuideTeal, RoundedCornerShape(50))
                    ) {
                        Text("${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = step,
                        fontSize = 14.sp,
                        color = GuideInk,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (chunk.doNot.isNotEmpty()) {
            DetailCard(title = "やってはいけないこと", titleColor = GuideRed) {
                chunk.doNot.forEach { item ->
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                        Text(
                            "✕",
                            fontSize = 13.sp,
                            color = GuideRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp),
                        )
                        Text(
                            text = item,
                            fontSize = 14.sp,
                            color = GuideInk,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f),
                        )
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
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFECEFF2), RoundedCornerShape(14.dp))
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

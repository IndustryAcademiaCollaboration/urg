package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urg.edge.KnowledgeChunk
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import kotlin.math.roundToInt
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
private sealed interface GuideScreen {
    object CategoryList : GuideScreen
    data class SubcategoryList(val category: String) : GuideScreen
    data class ChunkList(val category: String, val subcategory: String) : GuideScreen
    data class ChunkDetail(val chunk: KnowledgeChunk) : GuideScreen
}
private val disasterPhases = listOf(
    "平常時",
    "災害発生直後",
    "災害後数時間",
    "災害後数日",
    "災害後1週間",
    "災害後長期",
)
@Composable
fun FirstAidGuideScreen(
    chunks: List<KnowledgeChunk>,
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

    Column(modifier = modifier.fillMaxSize()) {
        // ヘッダー
        GuideHeader(
            screen = screen,
            onBack = {
                screen = when (val s = screen) {
                    is GuideScreen.CategoryList     -> s
                    is GuideScreen.SubcategoryList  -> GuideScreen.CategoryList
                    is GuideScreen.ChunkList        -> GuideScreen.SubcategoryList(s.category)
                    is GuideScreen.ChunkDetail      -> GuideScreen.ChunkList(
                        s.chunk.category, s.chunk.subcategory
                    )
                }
            }
        )

        HorizontalDivider()

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

        // コンテンツ
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
                        GuideCard(
                            title = category,
                            subtitle = "${filteredChunks.count { it.category == category }}項目",
                            onClick = { screen = GuideScreen.SubcategoryList(category) }
                        )
                    }
                }
            }

            is GuideScreen.SubcategoryList -> {
                val subcategories = remember(filteredChunks, s.category) {
                    filteredChunks
                        .filter { it.category == s.category }
                        .map { it.subcategory }
                        .distinct()
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(subcategories) { subcategory ->
                        GuideCard(
                            title = subcategory,
                            subtitle = "${filteredChunks.count { it.category == s.category && it.subcategory == subcategory }}項目",
                            onClick = { screen = GuideScreen.ChunkList(s.category, subcategory) }
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered) { chunk ->
                        GuideCard(
                            title = chunk.title,
                            subtitle = chunk.whenToUse,
                            severity = chunk.severity,
                            onClick = { screen = GuideScreen.ChunkDetail(chunk) }
                        )
                    }
                }
            }

            is GuideScreen.ChunkDetail -> {
                ChunkDetailView(chunk = s.chunk)
            }
        }
    }
}

@Composable
private fun GuideHeader(
    screen: GuideScreen,
    onBack: () -> Unit,
) {
    val title = when (screen) {
        is GuideScreen.CategoryList    -> "応急手当ガイド"
        is GuideScreen.SubcategoryList -> screen.category
        is GuideScreen.ChunkList       -> screen.subcategory
        is GuideScreen.ChunkDetail     -> screen.chunk.title
    }
    val showBack = screen !is GuideScreen.CategoryList

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        if (showBack) {
            TextButton(onClick = onBack) {
                Text("‹ 戻る", fontSize = 16.sp, color = Color(0xFF25B1BF))
            }
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun GuideCard(
    title: String,
    subtitle: String,
    severity: String? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (severity == "重症") {
                        Spacer(modifier = Modifier.width(8.dp))
                        SeverityBadge(severity)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2,
                )
            }
            Text(text = "›", fontSize = 20.sp, color = Color(0xFF25B1BF))
        }
    }
}

@Composable
private fun ChunkDetailView(chunk: KnowledgeChunk) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // いつ使う
        DetailSection(title = "こんなとき") {
            Text(text = chunk.whenToUse, fontSize = 14.sp)
        }

        // やること
        DetailSection(title = "やること") {
            chunk.steps.forEachIndexed { i, step ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "${i + 1}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF25B1BF),
                        modifier = Modifier.width(24.dp)
                    )
                    Text(text = step, fontSize = 14.sp)
                }
            }
        }

        // やってはいけないこと
        if (chunk.doNot.isNotEmpty()) {
            DetailSection(
                title = "やってはいけないこと",
                titleColor = Color(0xFFD32F2F),
            ) {
                chunk.doNot.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 13.sp,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.width(24.dp)
                        )
                        Text(text = item, fontSize = 14.sp)
                    }
                }
            }
        }

        // 重症バッジ
        if (chunk.severity == "重症") {
            SeverityBadge(chunk.severity!!)
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    titleColor: Color = Color(0xFF25B1BF),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(content = content)
    }
}

@Composable
private fun SeverityBadge(severity: String) {
    val color = if (severity == "重症") Color(0xFFD32F2F) else Color(0xFF388E3C)
    Text(
        text = severity,
        fontSize = 11.sp,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GuideSearchInput(
            value = query,
            onValueChange = onQueryChange,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            filterOptions.forEach { option ->
                FilterChip(
                    selected = selectedFilter == option,
                    onClick = { onFilterChange(option) },
                    label = {
                        Text(
                            text = option,
                            fontSize = 12.sp,
                        )
                    }
                )
            }
        }

        GuidePhaseSlider(
            selectedPhaseIndex = selectedPhaseIndex,
            onPhaseChange = onPhaseChange,
        )
    }
}

@Composable
private fun GuideSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F4F6), MaterialTheme.shapes.large)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = "応急手当を検索...",
                fontSize = 14.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(end = 36.dp)
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFF333333)
            ),
            cursorBrush = SolidColor(Color(0xFF25B1BF)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 36.dp)
        )

        if (value.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(24.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color(0xFFE0E0E0))
                    .clickable {
                        onValueChange("")
                    }
            ) {
                Text(
                    text = "×",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

private fun KnowledgeChunk.matchesGuideSearch(query: String): Boolean {
    val keywords = expandGuideSearchQuery(query)
    if (keywords.isEmpty()) return true

    val targetText = buildString {
        append(title)
        append(" ")
        append(category)
        append(" ")
        append(subcategory)
        append(" ")
        append(whenToUse)
        append(" ")
        append(severity.orEmpty())
        append(" ")
        append(steps.joinToString(" "))
        append(" ")
        append(doNot.joinToString(" "))
    }.lowercase()

    return keywords.any { keyword ->
        targetText.contains(keyword.lowercase())
    }
}

private fun KnowledgeChunk.matchesGuideFilter(filter: String): Boolean {
    return when (filter) {
        "すべて" -> true
        "重症" -> severity == "重症"
        else -> category == filter || subcategory == filter
    }
}

private fun expandGuideSearchQuery(query: String): List<String> {
    val text = query.trim()
    if (text.isBlank()) return emptyList()

    val keywords = mutableListOf(text)

    if (text.contains("水") && (text.contains("出ない") || text.contains("でない"))) {
        keywords += "断水"
    }

    if (
        text.contains("電気") ||
        text.contains("明かり") ||
        text.contains("あかり") ||
        text.contains("停電")
    ) {
        keywords += "停電"
    }

    if (
        text.contains("充電") ||
        text.contains("バッテリー") ||
        text.contains("スマホ") ||
        text.contains("携帯")
    ) {
        keywords += "電池"
        keywords += "充電"
    }

    return keywords.distinct()
}

@Composable
private fun GuidePhaseSlider(
    selectedPhaseIndex: Int,
    onPhaseChange: (Int) -> Unit,
) {
    val safeIndex = selectedPhaseIndex.coerceIn(0, disasterPhases.lastIndex)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = disasterPhases[safeIndex],
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF25B1BF)
        )

        Slider(
            value = safeIndex.toFloat(),
            onValueChange = { value ->
                onPhaseChange(
                    value.roundToInt().coerceIn(0, disasterPhases.lastIndex)
                )
            },
            valueRange = 0f..disasterPhases.lastIndex.toFloat(),
            steps = disasterPhases.size - 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            disasterPhases.forEach { phase ->
                Text(
                    text = phase,
                    fontSize = 8.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    modifier = Modifier.width(52.dp)
                )
            }
        }
    }
}
private fun KnowledgeChunk.matchesPhase(phaseTag: String): Boolean {
    if (tags.isEmpty()) return true
    return tags.contains(phaseTag)
}
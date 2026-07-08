package com.urg.edge.ui

import androidx.compose.foundation.background
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

@Composable
fun FirstAidGuideScreen(
    chunks: List<KnowledgeChunk>,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    var screen by remember { mutableStateOf<GuideScreen>(GuideScreen.CategoryList) }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFFF2F4F6))) {
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

        // コンテンツ
        when (val s = screen) {
            is GuideScreen.CategoryList -> {
                val categories = remember(chunks) {
                    chunks.map { it.category }.distinct()
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(categories) { category ->
                        GuideCard(
                            title = category,
                            subtitle = "${chunks.count { it.category == category }}項目",
                            onClick = { screen = GuideScreen.SubcategoryList(category) }
                        )
                    }
                }
            }

            is GuideScreen.SubcategoryList -> {
                val subcategories = remember(chunks, s.category) {
                    chunks.filter { it.category == s.category }
                        .map { it.subcategory }.distinct()
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(subcategories) { subcategory ->
                        GuideCard(
                            title = subcategory,
                            subtitle = "${chunks.count { it.category == s.category && it.subcategory == subcategory }}項目",
                            onClick = { screen = GuideScreen.ChunkList(s.category, subcategory) }
                        )
                    }
                }
            }

            is GuideScreen.ChunkList -> {
                val filtered = remember(chunks, s.category, s.subcategory) {
                    chunks.filter { it.category == s.category && it.subcategory == s.subcategory }
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
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
                ChunkDetailView(chunk = s.chunk, bottomPadding = bottomPadding)
            }
        }
    }
}

private val GuideTeal = Color(0xFF25B1BF)
private val GuideInk  = Color(0xFF10202A)

@Composable
private fun GuideHeader(
    screen: GuideScreen,
    onBack: () -> Unit,
) {
    val isRoot   = screen is GuideScreen.CategoryList
    val subTitle = when (screen) {
        is GuideScreen.SubcategoryList -> screen.category
        is GuideScreen.ChunkList       -> screen.subcategory
        is GuideScreen.ChunkDetail     -> screen.chunk.title
        else                           -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F4F6))
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "応急手当ガイド",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = GuideInk,
                modifier = Modifier.weight(1f),
            )
            if (!isRoot && subTitle != null) {
                TextButton(onClick = onBack) {
                    Text("‹ 戻る", fontSize = 14.sp, color = GuideTeal)
                }
            }
        }
        if (!isRoot && subTitle != null) {
            Text(
                text = subTitle,
                fontSize = 14.sp,
                color = Color(0xFF788E98),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
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
private fun ChunkDetailView(
    chunk: KnowledgeChunk,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
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

        Spacer(modifier = Modifier.height(bottomPadding))
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
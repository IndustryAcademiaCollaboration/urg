package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_triage_selected

// ─── デフォルト履歴データ ─────────────────────────────────
private data class TriageRecord(
    val id: String,
    val level: TriageLevel,
)

private val defaultRecords = listOf(
    TriageRecord("患者 #001", TriageLevel.GREEN),
    TriageRecord("患者 #002", TriageLevel.YELLOW),
    TriageRecord("患者 #003", TriageLevel.RED),
    TriageRecord("患者 #004", TriageLevel.BLACK),
)

// ─── トリアージ画面 ───────────────────────────────────────
@Composable
fun TriageScreen(
    onStartTriage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // トップバー
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "トリアージ",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF25B1BF),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ─── 新規トリアージ開始ボタン ───────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color(0xFF25B1BF).copy(alpha = 0.5f),
                        ambientColor = Color(0xFF25B1BF).copy(alpha = 0.3f),
                    )
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onStartTriage() }
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_triage_selected),
                    contentDescription = "トリアージ開始",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "新規トリアージ開始",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "患者の状態を評価します",
                        fontSize = 13.sp,
                        color = Color(0xFF788E98),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider(color = Color(0xFFF0F0F0))

            Spacer(modifier = Modifier.height(24.dp))

            // ─── 診断履歴セクション ──────────────────────────
            Text(
                text = "診断履歴",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF788E98),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            defaultRecords.forEach { record ->
                TriageRecordCard(
                    record = record,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ─── 診断履歴カード ───────────────────────────────────────
@Composable
private fun TriageRecordCard(
    record: TriageRecord,
    modifier: Modifier = Modifier,
) {
    val level = record.level
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = level.shadowColor.copy(alpha = 0.5f),
                ambientColor = level.shadowColor.copy(alpha = 0.25f),
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左カラーバー
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(level.labelColor)
            )

            // 患者ID + トリアージバッジ
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = record.id,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                )
                Spacer(modifier = Modifier.width(14.dp))
                // トリアージレベルバッジ
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(
                            level.labelColor.copy(alpha = 0.15f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = level.severityLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = level.labelColor,
                    )
                }
            }
        }
    }
}

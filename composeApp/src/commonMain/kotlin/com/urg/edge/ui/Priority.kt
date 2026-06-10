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
import urg.composeapp.generated.resources.ic_back

// ─── 優先度レベル ─────────────────────────────────────────
enum class PriorityLevel(
    val label: String,
    val badgeColor: Color,
    val badgeBg: Color,
    val borderColor: Color,
) {
    LOW(
        label = "低",
        badgeColor = Color(0xFF2E7D32),
        badgeBg = Color(0xFFA8E6C1),
        borderColor = Color(0xFF6AE2A7),
    ),
    MID(
        label = "中",
        badgeColor = Color(0xFFB58500),
        badgeBg = Color(0xFFFFE58F),
        borderColor = Color(0xFFFFCC44),
    ),
    HIGH(
        label = "高",
        badgeColor = Color(0xFFC62828),
        badgeBg = Color(0xFFF8B4B4),
        borderColor = Color(0xFFF47C7C),
    ),
}

// ─── デフォルトデータ ─────────────────────────────────────
private data class Patient(val name: String, val priority: PriorityLevel)

private val defaultPatients = listOf(
    Patient("患者 #001", PriorityLevel.LOW),
    Patient("患者 #002", PriorityLevel.MID),
    Patient("患者 #003", PriorityLevel.HIGH),
)

// ─── 優先度画面 ───────────────────────────────────────────
@Composable
fun PriorityScreen(
    onBack: () -> Unit,
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
                .padding(horizontal = 16.dp, vertical = 6.dp)
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
                text = "優先度",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF25B1BF),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 患者リスト
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 32.dp, bottom = 24.dp)
        ) {
            defaultPatients.forEach { patient ->
                PatientCard(
                    patient = patient,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ─── 患者カード ───────────────────────────────────────────
@Composable
private fun PatientCard(
    patient: Patient,
    modifier: Modifier = Modifier,
) {
    val p = patient.priority
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = p.borderColor.copy(alpha = 0.6f),
                ambientColor = p.borderColor.copy(alpha = 0.3f),
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左カラー
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(p.borderColor)
            )

            // 患者名 + バッジ
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = patient.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                )
                Spacer(modifier = Modifier.width(16.dp))

                // 優先度バッジ
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(p.badgeBg, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = p.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.badgeColor,
                    )
                }
            }
        }
    }
}
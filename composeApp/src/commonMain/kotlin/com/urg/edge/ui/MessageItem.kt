package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urg.edge.Message

import com.urg.edge.TriageResult
import com.urg.edge.VictimRecord
import androidx.compose.runtime.remember

@Composable
fun MessageItem(message: Message, victims: List<VictimRecord> = emptyList()) {
    val isUser = message.role == "user"
    val isSystem = message.text.startsWith("[初期化]") || message.text.startsWith("[初期化エラー]")

    val patientIds = remember(message.text) {
        val regex = Regex("""^P(\d+)\s*:""", RegexOption.MULTILINE)
        regex.findAll(message.text).map { it.groupValues[1].toInt() }.distinct().toList()
    }

    when {
        // ─── システム・ステータスチップ ─────────────────────────
        isSystem -> {
            val displayText = message.text
                .removePrefix("[初期化エラー] ")
                .removePrefix("[初期化] ")
            val isError = message.text.startsWith("[初期化エラー]")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isError) Color(0xFFFFECEC) else Color(0xFFEAF7F0),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = (if (isError) "✗ " else "✓ ") + displayText,
                        fontSize = 13.sp,
                        color = if (isError) Color(0xFFEF5350) else Color(0xFF555555),
                    )
                }
            }
        }

        // ─── ユーザーメッセージ（右） ───────────────────────────
        isUser -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .background(
                            Color(0xFF25B1BF),
                            RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message.text,
                        fontSize = 15.sp,
                        color = Color.White,
                    )
                }
            }
        }

        // ─── アシスタントメッセージ（左） ──────────────────────
        else -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                // バブル
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .background(
                            Color.White,
                            RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        if (patientIds.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                patientIds.forEach { id ->
                                    val v = victims.find { it.displayNo == id }
                                    PatientSeverityChip(
                                        displayNo = id,
                                        isSevere = v?.result == TriageResult.SEVERE
                                    )
                                }
                            }
                        }
                        Text(
                            text = message.text,
                            fontSize = 15.sp,
                            color = Color(0xFF333333),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PatientSeverityChip(
    displayNo: Int,
    isSevere: Boolean,
    modifier: Modifier = Modifier
) {
    val themeColor = if (isSevere) Color(0xFFE24B4A) else Color(0xFF25B1BF)
    val bgColor    = if (isSevere) Color(0xFFFCEBEB) else Color(0xFFE8F9FA)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(themeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "P$displayNo",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = if (isSevere) "重症" else "軽症",
            color = themeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}

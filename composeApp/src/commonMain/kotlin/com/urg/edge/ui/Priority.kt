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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urg.edge.PatientNote
import com.urg.edge.TriageResult
import com.urg.edge.VictimRecord
import com.urg.edge.toTimeString
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityScreen(
    victims: List<VictimRecord>,
    onBack: () -> Unit,
    onUpdateNote: (victimId: String, note: PatientNote) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingVictim by remember { mutableStateOf<VictimRecord?>(null) }
    
    // 作成時間でソート
    val sortedVictims = remember(victims) {
        victims.sortedBy { it.recordedAt }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F6))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = "優先度",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF25B1BF),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (sortedVictims.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "トリアージ記録がありません",
                    fontSize = 16.sp,
                    color = Color(0xFF788E98)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 32.dp, bottom = 24.dp)
            ) {
                sortedVictims.forEachIndexed { index, victim ->
                    VictimCard(
                        number = index + 1,
                        victim = victim,
                        onClick = { editingVictim = victim },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    editingVictim?.let { victim ->
        PatientNoteEditSheet(
            victim = victim,
            onDismiss = { editingVictim = null },
            onSave = { note ->
                onUpdateNote(victim.id, note)
                editingVictim = null
            }
        )
    }
}

@Composable
private fun VictimCard(
    number: Int,
    victim: VictimRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // トリアージ結果: MINOR -> "軽症", SEVERE -> "重症"
    val isSevere = victim.result == TriageResult.SEVERE
    
    val label = if (isSevere) "重症" else "軽症"
    val stripColor = if (isSevere) Color(0xFFEE6055) else Color(0xFF60D394)
    val badgeBgColor = if (isSevere) Color(0xFFFEE4E2) else Color(0xFFAFEBC6)
    val badgeTextColor = if (isSevere) Color(0xFFEE6055) else Color(0xFF148752)

    val shadowColor = if (isSevere) Color(0xFFEE6055).copy(alpha = 0.3f) else Color(0xFF60D394).copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, end = 6.dp) // 陰影が表示されるスペースを確保
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = shadowColor,
                ambientColor = shadowColor,
            )
            .background(Color.White, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側のカラーバー
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(stripColor)
            )
            
            Column(
                modifier = Modifier
                    .padding(start = 20.dp, end = 16.dp)
                    .weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "傷病者 #${number.toString().padStart(3, '0')}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 優先度バッジ (影付き)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(badgeBgColor, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // サブテキスト (場所・特徴)
                val noteText = listOfNotNull(victim.note?.location, victim.note?.feature)
                    .joinToString(" · ")
                val subText = buildString {
                    append(victim.recordedAt.toTimeString())
                    if (noteText.isNotEmpty()) append("  $noteText")
                }
                
                Text(
                    text = subText,
                    fontSize = 12.sp,
                    color = Color(0xFF788E98),
                )
            }

            Text(
                text = "編集",
                fontSize = 11.sp,
                color = Color(0xFF25B1BF),
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientNoteEditSheet(
    victim: VictimRecord,
    onDismiss: () -> Unit,
    onSave: (PatientNote) -> Unit,
) {
    var location by remember { mutableStateOf(victim.note?.location ?: "") }
    var feature  by remember { mutableStateOf(victim.note?.feature  ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "患者情報の追加",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("場所メモ") },
                placeholder = { Text("例：B棟前") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = feature,
                onValueChange = { feature = it },
                label = { Text("特徴メモ") },
                placeholder = { Text("例：赤い服の男性") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    onSave(PatientNote(
                        location = location.ifBlank { null },
                        feature  = feature.ifBlank { null }
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25B1BF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存", fontSize = 15.sp)
            }
        }
    }
}

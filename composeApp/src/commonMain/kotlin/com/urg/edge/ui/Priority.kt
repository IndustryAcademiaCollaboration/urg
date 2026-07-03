package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urg.edge.PatientNote
import com.urg.edge.TriageResult
import com.urg.edge.VictimRecord
import com.urg.edge.toTimeString
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_gray_close
import urg.composeapp.generated.resources.ic_pencil
import urg.composeapp.generated.resources.ic_place
import urg.composeapp.generated.resources.ic_tag
import urg.composeapp.generated.resources.ic_trash

// ─── Colors ───────────────────────────────────────────────────────────────────
private val SevereColor = Color(0xFFE5463F)
private val MinorColor  = Color(0xFF16A36B)
private val PrioTeal    = Color(0xFF25B1BF)
private val PrioGray    = Color(0xFF94A6B0)
private val PrioInk     = Color(0xFF10202A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityScreen(
    victims: List<VictimRecord>,
    victimNumbers: Map<String, Int> = emptyMap(),
    onBack: () -> Unit,
    onUpdateNote: (victimId: String, note: PatientNote) -> Unit,
    onDeleteVictim: (victimId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var editingVictim       by remember { mutableStateOf<VictimRecord?>(null) }
    var deleteConfirmTarget by remember { mutableStateOf<VictimRecord?>(null) }
    var sortBySeverity      by remember { mutableStateOf(false) }

    // 番号は ViewModel から渡された victimNumbers を使用（削除後も安定）
    val numberMap = victimNumbers

    val sortedVictims = remember(victims, sortBySeverity) {
        if (sortBySeverity) {
            victims.sortedWith(
                compareByDescending<VictimRecord> { it.result == TriageResult.SEVERE }
                    .thenBy { it.recordedAt }
            )
        } else {
            victims.sortedBy { it.recordedAt }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F6))
    ) {
        // ── ヘッダー ─────────────────────────────────────────────────
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
                        .background(PrioTeal)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PRIORITY LIST",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrioTeal,
                    letterSpacing = 1.5.sp,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "優先度",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = PrioInk,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sortedVictims.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "トリアージ記録がありません",
                    fontSize = 14.sp,
                    color = PrioGray,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Sort By ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "SORT BY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrioGray,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f),
                    )
                    // 時間
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (!sortBySeverity) PrioTeal else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (!sortBySeverity) Color.Transparent else PrioGray,
                                shape = RoundedCornerShape(50),
                            )
                            .clickable { sortBySeverity = false }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "時間",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (!sortBySeverity) Color.White else PrioGray,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // 重症度
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sortBySeverity) PrioTeal else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (sortBySeverity) Color.Transparent else PrioGray,
                                shape = RoundedCornerShape(50),
                            )
                            .clickable { sortBySeverity = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "重症度",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (sortBySeverity) Color.White else PrioGray,
                        )
                    }
                }

                // ── カードリスト ─────────────────────────────────────
                sortedVictims.forEach { victim ->
                    VictimCard(
                        number = numberMap[victim.id] ?: 0,
                        victim = victim,
                        onEdit = { editingVictim = victim },
                        onDelete = { deleteConfirmTarget = victim },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    editingVictim?.let { victim ->
        PatientNoteEditSheet(
            victim = victim,
            number = numberMap[victim.id] ?: 0,
            onDismiss = { editingVictim = null },
            onSave = { note ->
                onUpdateNote(victim.id, note)
                editingVictim = null
            }
        )
    }

    deleteConfirmTarget?.let { victim ->
        DeleteConfirmDialog(
            number = numberMap[victim.id] ?: 0,
            onDismiss = { deleteConfirmTarget = null },
            onConfirm = {
                onDeleteVictim(victim.id)
                deleteConfirmTarget = null
            }
        )
    }
}

// ─── Victim Card ──────────────────────────────────────────────────────────────
@Composable
private fun VictimCard(
    number: Int,
    victim: VictimRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSevere    = victim.result == TriageResult.SEVERE
    val label       = if (isSevere) "重症" else "軽症"
    val accentColor = if (isSevere) SevereColor else MinorColor
    val numStr      = "#${number.toString().padStart(3, '0')}"
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左カラーボーダー
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(accentColor)
        )

        Column(
            modifier = Modifier
                .padding(start = 16.dp, top = 20.dp, bottom = 20.dp)
                .weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = numStr,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(accentColor, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = victim.recordedAt.toTimeString(),
                fontSize = 14.sp,
                color = PrioGray,
            )
            val noteText = listOfNotNull(victim.note?.location, victim.note?.feature)
                .joinToString("・")
            if (noteText.isNotEmpty()) {
                Text(
                    text = noteText,
                    fontSize = 14.sp,
                    color = PrioTeal,
                )
            }
        }

        // 右: ⋮ メニュー
        Box(contentAlignment = Alignment.TopEnd) {
            Text(
                text = "⋮",
                fontSize = 24.sp,
                color = PrioGray,
                modifier = Modifier
                    .clickable { menuExpanded = true }
                    .padding(horizontal = 16.dp, vertical = 20.dp),
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                DropdownMenuItem(
                    text = {
                        Text(text = "編集", fontSize = 15.sp, color = Color(0xFF566876))
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_pencil),
                            contentDescription = null,
                            tint = Color(0xFF566876),
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    },
                    modifier = Modifier.background(Color.White),
                )
                HorizontalDivider(
                    color = Color(0xFFEEEEEE),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                DropdownMenuItem(
                    text = {
                        Text(text = "削除", fontSize = 15.sp, color = Color(0xFFDE3F39))
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_trash),
                            contentDescription = null,
                            tint = Color(0xFFDE3F39),
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                    modifier = Modifier.background(Color.White),
                )
            }
        }
    }
}

// ─── 患者ノート編集シート ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientNoteEditSheet(
    victim: VictimRecord,
    number: Int,
    onDismiss: () -> Unit,
    onSave: (PatientNote) -> Unit,
) {
    var location by remember { mutableStateOf(victim.note?.location ?: "") }
    var feature  by remember { mutableStateOf(victim.note?.feature  ?: "") }
    val numStr   = "#${number.toString().padStart(3, '0')}"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── ヘッダー ────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 編集アイコン（teal 角丸）
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE2F5F7))
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_pencil),
                        contentDescription = null,
                        tint = PrioTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EDIT RECORD",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrioTeal,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = "$numStr の編集",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrioInk,
                    )
                }
                // X ボタン
                Icon(
                    painter = painterResource(Res.drawable.ic_gray_close),
                    contentDescription = "閉じる",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 場所メモ ─────────────────────────────────────────────
            Text(
                text = "場所メモ",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrioGray,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFDCE3E8), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_place),
                    contentDescription = null,
                    tint = PrioTeal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = location,
                    onValueChange = { location = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp, color = PrioInk),
                    cursorBrush = SolidColor(PrioTeal),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (location.isEmpty()) {
                            Text("例：B棟前", fontSize = 16.sp, color = PrioGray)
                        }
                        inner()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 特徴メモ ─────────────────────────────────────────────
            Text(
                text = "特徴メモ",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrioGray,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFDCE3E8), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_tag),
                    contentDescription = null,
                    tint = PrioTeal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = feature,
                    onValueChange = { feature = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp, color = PrioInk),
                    cursorBrush = SolidColor(PrioTeal),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (feature.isEmpty()) {
                            Text("例：赤い服の男性", fontSize = 16.sp, color = PrioGray)
                        }
                        inner()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 保存ボタン ────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrioTeal)
                    .clickable {
                        onSave(PatientNote(
                            location = location.ifBlank { null },
                            feature  = feature.ifBlank { null }
                        ))
                    }
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = "保存",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

// ─── 削除確認ダイアログ ────────────────────────────────────────────────────────
@Composable
private fun DeleteConfirmDialog(
    number: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val numStr = "#${number.toString().padStart(3, '0')}"
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ゴミ箱アイコン（薄ピンク丸 + 赤アイコン）
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFFE5E4))
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_trash),
                    contentDescription = null,
                    tint = Color(0xFFDE3F39),
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "記録を削除しますか？",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrioInk,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "傷病者 $numStr の記録を削除します。この操作は取り消せません。",
                fontSize = 13.sp,
                color = Color(0xFF566876),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // キャンセル
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF2F4F6))
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp)
                ) {
                    Text(
                        text = "キャンセル",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF566876),
                    )
                }
                // 削除
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFDE3F39))
                        .clickable { onConfirm() }
                        .padding(vertical = 14.dp)
                ) {
                    Text(
                        text = "削除",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

package com.urg.edge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_manual

private val SettingsTeal = Color(0xFF25B1BF)
private val SettingsInk  = Color(0xFF10202A)

@Composable
fun SettingsScreen(
    onManualClick: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F6))
            .padding(bottom = bottomPadding)
    ) {
        // ── ヘッダー ──────────────────────────────────────────────────────
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
                        .background(SettingsTeal)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OTHER",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SettingsTeal,
                    letterSpacing = 1.5.sp,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "その他",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = SettingsInk,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── メニューカード ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            // マニュアル行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onManualClick() }
                    .padding(horizontal = 30.dp, vertical = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // アイコン背景
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE2F5F7))
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_manual),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = "マニュアル",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = SettingsInk,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

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
import androidx.compose.foundation.layout.statusBarsPadding
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

@Composable
fun SettingsScreen(
    onManualClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val teal = Color(0xFF25B1BF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F6))
    ) {
        // ── ヘッダー ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = "その他",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = teal,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(Modifier.height(32.dp))

        // ── マニュアル ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .clickable { onManualClick() }
                .padding(horizontal = 30.dp, vertical = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_manual),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(20.dp))
            Text(
                text = "マニュアル",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1A1A1A),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

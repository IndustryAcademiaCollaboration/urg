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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_back
import urg.composeapp.generated.resources.ic_calm
import urg.composeapp.generated.resources.ic_hurry

@Composable
fun ModeSelect(
    onBack: () -> Unit,
    onCalmClick: () -> Unit,
    onHurryClick: () -> Unit,
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
                text = "モード選択",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF25B1BF),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 説明テキスト（左揃え）
        Text(
            text = "現在の状況を選択してください",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 余裕ありモードカード
        ModeOptionCard(
            label = "余裕ありモード",
            icon = Res.drawable.ic_calm,
            shadowColor = Color(0xFF69F0AE),
            onClick = onCalmClick,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 焦りモードカード
        ModeOptionCard(
            label = "焦りモード",
            icon = Res.drawable.ic_hurry,
            shadowColor = Color(0xFFFF8A80),
            onClick = onHurryClick,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun ModeOptionCard(
    label: String,
    icon: DrawableResource,
    shadowColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = shadowColor,
                ambientColor = shadowColor
            )
            .background(Color.White, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 50.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = Color.Unspecified,
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.width(50.dp))
        Text(
            text = label,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333)
        )
    }
}

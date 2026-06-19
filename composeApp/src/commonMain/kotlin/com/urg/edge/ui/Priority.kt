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
import com.urg.edge.TriageResult
import com.urg.edge.VictimRecord
import org.jetbrains.compose.resources.painterResource
import urg.composeapp.generated.resources.Res
import urg.composeapp.generated.resources.ic_back

private val SevereBorderColor = Color(0xFFF47C7C)
private val MinorBorderColor  = Color(0xFF6AE2A7)

@Composable
fun PriorityScreen(
    victims: List<VictimRecord>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
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

        if (victims.isEmpty()) {
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
                victims.forEachIndexed { index, victim ->
                    VictimCard(
                        number = index + 1,
                        victim = victim,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun VictimCard(
    number: Int,
    victim: VictimRecord,
    modifier: Modifier = Modifier,
) {
    val isSevere = victim.result == TriageResult.SEVERE
    val borderColor = if (isSevere) SevereBorderColor else MinorBorderColor
    val badgeBg     = if (isSevere) Color(0xFFF8B4B4) else Color(0xFFA8E6C1)
    val badgeColor  = if (isSevere) Color(0xFFC62828) else Color(0xFF2E7D32)
    val label       = if (isSevere) "重症" else "軽症"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = borderColor.copy(alpha = 0.6f),
                ambientColor = borderColor.copy(alpha = 0.3f),
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(borderColor)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "患者 #${String.format("%03d", number)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                    )
                }
                victim.memo?.let { memo ->
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = memo,
                        fontSize = 12.sp,
                        color = Color(0xFF788E98),
                    )
                }
            }
        }
    }
}

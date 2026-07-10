package com.urg.edge.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.urg.edge.VictimRecord

@Composable
expect fun MapScreen(
    victims: List<VictimRecord> = emptyList(),
    focusedVictimId: String? = null,
    modifier: Modifier = Modifier,
)
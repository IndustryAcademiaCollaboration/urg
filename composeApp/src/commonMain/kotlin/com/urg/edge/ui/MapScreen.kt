package com.urg.edge.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
expect fun MapScreen(
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
)

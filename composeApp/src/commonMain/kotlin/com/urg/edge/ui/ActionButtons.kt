package com.urg.edge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.urg.edge.Strings

@Composable
fun ActionButtons(onTriageClick: () -> Unit, onSendClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onTriageClick, modifier = Modifier.weight(1f)) {
            Text(Strings.BUTTON_TRIAGE)
        }
        Button(onClick = onSendClick, modifier = Modifier.weight(1f)) {
            Text(Strings.BUTTON_SEND)
        }
    }
}

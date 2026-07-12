package com.urg.edge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.urg.edge.Message

import com.urg.edge.VictimRecord

@Composable
fun MessageList(
    messages: List<Message>,
    streamingText: String,
    victims: List<VictimRecord> = emptyList(),
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 画面初回表示時（他画面から戻った時も含む）即座に最下部へ
    LaunchedEffect(Unit) {
        val total = messages.size + if (streamingText.isNotEmpty()) 1 else 0
        if (total > 0) listState.scrollToItem(total - 1)
    }

    // メッセージ追加・ストリーミング中はアニメーションで最下部へ
    LaunchedEffect(messages.size, streamingText.isNotEmpty()) {
        val total = messages.size + if (streamingText.isNotEmpty()) 1 else 0
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message -> MessageItem(message, victims) }
        if (streamingText.isNotEmpty()) {
            item { MessageItem(Message("assistant", streamingText), victims) }
        }
    }
}

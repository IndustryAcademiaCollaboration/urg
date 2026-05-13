package com.urg.edge

data class KnowledgeChunk(
    val id: String,
    val category: String,
    val title: String,
    val text: String,
    val guidance: String? = null,
    val severity: String? = null
)

package com.urg.edge

data class KnowledgeChunk(
    val id: String,
    val category: String,
    val subcategory: String,
    val context: List<String>,
    val title: String,
    val keywords: List<String>,
    val whenToUse: String,
    val steps: List<String>,
    val doNot: List<String> = emptyList(),
    val severity: String? = null,
    val next: String? = null
) {
    fun toEmbeddingText(): String =
        "$category $subcategory $title ${keywords.joinToString(" ")} $whenToUse ${steps.joinToString(" ")}"

    fun toPromptText(): String {
        val stepsText = steps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString(" ")
        val doNotText = if (doNot.isNotEmpty()) " 禁止：${doNot.joinToString("、")}" else ""
        return "【$title】$whenToUse。$stepsText$doNotText"
    }
}
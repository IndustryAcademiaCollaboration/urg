package com.urg.edge

class KeywordRetriever(private val chunks: List<KnowledgeChunk>) {

    fun retrieve(query: String, topK: Int = 3): List<KnowledgeChunk> {
        val queryBigrams = bigrams(query)
        if (queryBigrams.isEmpty()) return emptyList()

        return chunks
            .map { chunk ->
                val chunkBigrams = bigrams("${chunk.title} ${chunk.text}")
                val score = queryBigrams.intersect(chunkBigrams).size
                chunk to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    // 日本語は単語分割が困難なため、2文字のbigramで近似的にキーワードマッチングする
    private fun bigrams(text: String): Set<String> {
        val cleaned = text.replace("\\s+".toRegex(), "")
        return (0 until cleaned.length - 1)
            .map { cleaned.substring(it, it + 2) }
            .toSet()
    }
}

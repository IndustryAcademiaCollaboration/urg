package com.urg.edge

interface KnowledgeRetriever : AutoCloseable {
    fun retrieve(query: String, topK: Int = 3): List<KnowledgeChunk>
}

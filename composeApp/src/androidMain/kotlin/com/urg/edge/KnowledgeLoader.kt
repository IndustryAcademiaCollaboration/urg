package com.urg.edge

import android.content.res.AssetManager
import org.json.JSONObject

object KnowledgeLoader {

    fun load(assets: AssetManager): List<KnowledgeChunk> {
        val fileNames = assets.list("knowledge") ?: return emptyList()
        return fileNames
            .filter { it.endsWith(".json") }
            .flatMap { fileName -> parseFile(assets, fileName) }
    }

    private fun parseFile(assets: AssetManager, fileName: String): List<KnowledgeChunk> {
        val json = assets.open("knowledge/$fileName").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val category = root.getString("category")
        val chunksArray = root.getJSONArray("chunks")

        return (0 until chunksArray.length()).map { i ->
            val obj = chunksArray.getJSONObject(i)
            KnowledgeChunk(
                id = obj.getString("id"),
                category = category,
                title = obj.getString("title"),
                text = obj.getString("text"),
                guidance = obj.optString("guidance").takeIf { it.isNotEmpty() },
                severity = obj.optString("severity").takeIf { it.isNotEmpty() }
            )
        }
    }
}

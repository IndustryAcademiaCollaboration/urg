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
        val subcategory = root.getString("subcategory")
        val contextArr = root.getJSONArray("context")
        val context = (0 until contextArr.length()).map { contextArr.getString(it) }
        val chunksArray = root.getJSONArray("chunks")

        return (0 until chunksArray.length()).map { i ->
            val obj = chunksArray.getJSONObject(i)

            val keywordsArr = obj.getJSONArray("keywords")
            val keywords = (0 until keywordsArr.length()).map { keywordsArr.getString(it) }

            val stepsArr = obj.getJSONArray("steps")
            val steps = (0 until stepsArr.length()).map { stepsArr.getString(it) }

            val doNotArr = obj.optJSONArray("do_not")
            val doNot = if (doNotArr != null) {
                (0 until doNotArr.length()).map { doNotArr.getString(it) }
            } else {
                emptyList()
            }

            KnowledgeChunk(
                id = obj.getString("id"),
                category = category,
                subcategory = subcategory,
                context = context,
                title = obj.getString("title"),
                keywords = keywords,
                whenToUse = obj.getString("when"),
                steps = steps,
                doNot = doNot,
                severity = obj.optString("severity").takeIf { it.isNotEmpty() },
                next = obj.optString("next").takeIf { it.isNotEmpty() }
            )
        }
    }
}
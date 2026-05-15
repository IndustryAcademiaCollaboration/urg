package com.urg.edge

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import java.io.File
import java.nio.LongBuffer
import kotlin.math.sqrt

class EmbeddingRetriever(
    context: Context,
    private val chunks: List<KnowledgeChunk>
) : KnowledgeRetriever, AutoCloseable {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val tokenizer: HuggingFaceTokenizer
    private val chunkEmbeddings: List<FloatArray>

    init {
        // readBytes() で JVM ヒープに展開すると OOM になるため、ファイルパス渡しで mmap を使う
        val modelFile = File(context.filesDir, "multilingual-e5-small-int8.onnx")
        if (!modelFile.exists()) {
            context.assets.open("models/multilingual-e5-small-int8.onnx").use { input ->
                modelFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        session = env.createSession(modelFile.absolutePath)

        // tokenizer.json は assets から cacheDir にコピーして path で読み込む
        val tokenizerFile = File(context.cacheDir, "tokenizer.json")
        if (!tokenizerFile.exists()) {
            context.assets.open("models/tokenizer.json").use { input ->
                tokenizerFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile.toPath(), mapOf(
            "truncation" to "true",
            "maxLength" to "512"
        ))

        chunkEmbeddings = chunks.map { chunk ->
            embed("passage: ${chunk.title} ${chunk.text}")
        }
        Log.d("EMBEDDING_INIT", "chunk embeddings computed: ${chunks.size} chunks")
    }

    override fun retrieve(query: String, topK: Int): List<KnowledgeChunk> {
        val queryEmbedding = embed("query: $query")
        return chunks
            .mapIndexed { i, chunk ->
                chunk to cosineSimilarity(queryEmbedding, chunkEmbeddings[i])
            }
            .filter { it.second > 0.3f }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    private fun embed(text: String): FloatArray {
        val encoding = tokenizer.encode(text)
        val ids = encoding.ids
        val attentionMask = encoding.attentionMask
        val seqLen = ids.size.toLong()

        val tokenTypeIds = LongArray(ids.size) { 0L }

        val inputIdsTensor = OnnxTensor.createTensor(
            env, LongBuffer.wrap(ids), longArrayOf(1L, seqLen)
        )
        val attentionMaskTensor = OnnxTensor.createTensor(
            env, LongBuffer.wrap(attentionMask), longArrayOf(1L, seqLen)
        )
        val tokenTypeIdsTensor = OnnxTensor.createTensor(
            env, LongBuffer.wrap(tokenTypeIds), longArrayOf(1L, seqLen)
        )

        session.run(mapOf(
            "input_ids" to inputIdsTensor,
            "attention_mask" to attentionMaskTensor,
            "token_type_ids" to tokenTypeIdsTensor
        )).use { result ->
            val hiddenState = (result[0].value as Array<Array<FloatArray>>)[0]
            return meanPool(hiddenState, attentionMask)
        }
    }

    private fun meanPool(hiddenState: Array<FloatArray>, attentionMask: LongArray): FloatArray {
        val hiddenSize = hiddenState[0].size
        val pooled = FloatArray(hiddenSize)
        var validTokens = 0

        for (i in hiddenState.indices) {
            if (attentionMask[i] == 1L) {
                for (j in hiddenState[i].indices) pooled[j] += hiddenState[i][j]
                validTokens++
            }
        }
        if (validTokens > 0) for (j in pooled.indices) pooled[j] /= validTokens
        return normalize(pooled)
    }

    private fun normalize(v: FloatArray): FloatArray {
        val norm = sqrt(v.sumOf { (it * it).toDouble() }.toFloat())
        return if (norm > 0f) FloatArray(v.size) { v[it] / norm } else v
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float =
        a.indices.sumOf { (a[it] * b[it]).toDouble() }.toFloat()

    override fun close() {
        session.close()
        env.close()
        tokenizer.close()
    }
}

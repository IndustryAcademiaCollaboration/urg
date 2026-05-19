package com.urg.edge.stt

interface SttEngine : AutoCloseable {
    fun recognize(samples: FloatArray): String
    override fun close()
}

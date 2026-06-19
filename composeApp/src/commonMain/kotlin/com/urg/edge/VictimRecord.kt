package com.urg.edge

data class VictimRecord(
    val id: String,
    val sessionId: String,
    val triageInput: TriageInput,
    val result: TriageResult,
    val actionPlan: TriageActionPlan,
    val memo: String? = null,
    val recordedAt: Long
)

package com.urg.edge

data class PatientNote(
    val location: String? = null,
    val feature: String? = null
)

data class VictimRecord(
    val id: String,
    val sessionId: String,
    val triageInput: TriageInput,
    val result: TriageResult,
    val actionPlan: TriageActionPlan,
    val note: PatientNote? = null,
    val recordedAt: Long
)

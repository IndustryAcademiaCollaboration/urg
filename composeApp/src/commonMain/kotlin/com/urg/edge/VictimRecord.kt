package com.urg.edge

data class PatientNote(
    val location: String? = null,
    val feature: String? = null
)

data class VictimRecord(
    val id: String,
    val sessionId: String,
    // セッション内で登録順に採番される不変ラベル（P1〜Pn の番号部分）。0 は未採番
    val displayNo: Int = 0,
    val triageInput: TriageInput,
    val result: TriageResult,
    val actionPlan: TriageActionPlan,
    val note: PatientNote? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val recordedAt: Long
)

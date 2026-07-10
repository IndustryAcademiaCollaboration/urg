package com.urg.edge

interface TriageSessionRepository {
    fun startSession(latitude: Double?, longitude: Double?): TriageSession
    fun getLatestSession(): TriageSession?
    fun saveVictim(victim: VictimRecord): VictimRecord
    fun getVictimsBySession(sessionId: String): List<VictimRecord>
    fun updateVictimNote(victimId: String, note: PatientNote)
    fun deleteVictim(victimId: String)
}

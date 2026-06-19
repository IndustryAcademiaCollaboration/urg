package com.urg.edge

interface TriageSessionRepository {
    fun startSession(latitude: Double?, longitude: Double?): TriageSession
    fun saveVictim(victim: VictimRecord)
    fun getVictimsBySession(sessionId: String): List<VictimRecord>
    fun getVictimsByPriority(): List<VictimRecord>
}

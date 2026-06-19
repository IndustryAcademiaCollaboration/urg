package com.urg.edge

import com.urg.edge.database.UrgDatabase
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class TriageSessionRepositoryImpl(
    private val database: UrgDatabase
) : TriageSessionRepository {

    @OptIn(ExperimentalTime::class)
    override fun startSession(latitude: Double?, longitude: Double?): TriageSession {
        val id = UUID.randomUUID().toString()
        val now = Clock.System.now().toEpochMilliseconds()
        database.clSessionsQueries.insert(id, latitude, longitude, now)
        return TriageSession(id = id, latitude = latitude, longitude = longitude, startedAt = now)
    }

    @OptIn(ExperimentalTime::class)
    override fun saveVictim(victim: VictimRecord) {
        database.clVictimsQueries.insert(
            id = victim.id,
            session_id = victim.sessionId,
            severity = victim.result.name.lowercase(),
            can_walk = victim.triageInput.canWalk?.toLong(),
            is_breathing = victim.triageInput.isBreathing?.toLong(),
            has_pulse = victim.triageInput.hasPulse?.toLong(),
            consciousness = victim.triageInput.isConscious?.toLong(),
            memo = victim.memo,
            recorded_at = victim.recordedAt
        )
    }

    override fun getVictimsBySession(sessionId: String): List<VictimRecord> =
        database.clVictimsQueries.selectBySession(sessionId).executeAsList().map { it.toVictimRecord() }

    override fun getVictimsByPriority(): List<VictimRecord> =
        database.clVictimsQueries.selectOrderedBySeverity().executeAsList().map { it.toVictimRecord() }

    private fun com.urg.edge.database.Cl_victims.toVictimRecord(): VictimRecord {
        val input = TriageInput(
            canWalk = can_walk?.toBooleanOrNull(),
            isBreathing = is_breathing?.toBooleanOrNull(),
            hasPulse = has_pulse?.toBooleanOrNull(),
            isConscious = consciousness?.toBooleanOrNull()
        )
        val result = when (severity) {
            "severe" -> TriageResult.SEVERE
            else -> TriageResult.MINOR
        }
        return VictimRecord(
            id = id,
            sessionId = session_id,
            triageInput = input,
            result = result,
            actionPlan = StartRuleEngine.decideActions(result, input),
            memo = memo,
            recordedAt = recorded_at
        )
    }

    private fun Long.toBooleanOrNull(): Boolean? = when (this) {
        1L -> true
        0L -> false
        else -> null
    }

    private fun Boolean.toLong(): Long = if (this) 1L else 0L
}

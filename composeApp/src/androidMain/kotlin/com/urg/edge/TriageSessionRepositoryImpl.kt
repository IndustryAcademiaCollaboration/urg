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

    override fun saveVictim(victim: VictimRecord): VictimRecord =
        // 採番（セッション内 max+1）と挿入を同一トランザクションで行い、ラベルを登録時に確定させる。
        // 制限: セッション内最大番号の対象者を削除した直後の登録では同じ番号が再利用される
        // （恒久対応はセッション側カウンタ列の追加。将来課題）
        database.clVictimsQueries.transactionWithResult {
            val next = database.clVictimsQueries
                .selectMaxDisplayNo(victim.sessionId).executeAsOne() + 1
            database.clVictimsQueries.insert(
                id = victim.id,
                session_id = victim.sessionId,
                display_no = next,
                severity = victim.result.name.lowercase(),
                can_walk = victim.triageInput.canWalk?.toLong(),
                is_breathing = victim.triageInput.isBreathing?.toLong(),
                has_pulse = victim.triageInput.hasPulse?.toLong(),
                consciousness = victim.triageInput.isConscious?.toLong(),
                location = victim.note?.location,
                feature = victim.note?.feature,
                recorded_at = victim.recordedAt
            )
            victim.copy(displayNo = next.toInt())
        }

    override fun updateVictimNote(victimId: String, note: PatientNote) {
        database.clVictimsQueries.updateNote(
            location = note.location,
            feature = note.feature,
            id = victimId
        )
    }

    override fun getVictimsBySession(sessionId: String): List<VictimRecord> =
        database.clVictimsQueries.selectBySession(sessionId).executeAsList().map { it.toVictimRecord() }

    override fun deleteVictim(victimId: String) {
        database.clVictimsQueries.deleteById(victimId)
    }

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
        val note = if (location == null && feature == null) null
                   else PatientNote(location = location, feature = feature)
        return VictimRecord(
            id = id,
            sessionId = session_id,
            displayNo = display_no.toInt(),
            triageInput = input,
            result = result,
            actionPlan = StartRuleEngine.decideActions(result, input),
            note = note,
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

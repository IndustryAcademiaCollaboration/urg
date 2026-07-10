package com.urg.edge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PromptBuilderVictimsContextTest {

    private fun victim(
        displayNo: Int,
        result: TriageResult,
        note: PatientNote? = null,
        input: TriageInput = TriageInput(),
        recordedAt: Long = displayNo.toLong(),
    ): VictimRecord = VictimRecord(
        id = "id-$displayNo",
        sessionId = "session-1",
        displayNo = displayNo,
        triageInput = input,
        result = result,
        actionPlan = StartRuleEngine.decideActions(result, input),
        note = note,
        recordedAt = recordedAt,
    )

    // ── クラス初期化順序（regression: presets に未初期化の null が混入しないこと）──

    @Test
    fun presetsはオブジェクト参照後でもnullを含まない() {
        // ChatScope.All を presets より先に参照する（実クラッシュと同じ初期化順序）
        val first: ChatScope = ChatScope.All
        val v = victim(displayNo = 1, result = TriageResult.SEVERE)
        assertEquals(2, ChatScope.presets.size)
        // 未初期化の null 要素が混ざっていれば matches() 呼び出しで NPE になる
        ChatScope.presets.forEach { it.matches(v) }
        assertTrue(first.matches(v))
    }

    // ── 書式 ──────────────────────────────────────────────────────────

    @Test
    fun フル属性の1行書式() {
        val v = victim(
            displayNo = 1,
            result = TriageResult.SEVERE,
            note = PatientNote(location = "玄関付近", feature = "右腕出血・意識あり"),
            input = TriageInput(canWalk = false, isBreathing = true, hasPulse = true, isConscious = true),
        )
        val context = PromptBuilder.buildVictimsContext(listOf(v), ChatScope.All)
        assertTrue(context.contains("P1｜重症｜場所:玄関付近｜見た目:右腕出血・意識あり｜歩行:不可｜呼吸:あり｜脈拍:あり｜意識:あり"))
    }

    @Test
    fun null属性はキーごと省略される() {
        val v = victim(displayNo = 2, result = TriageResult.MINOR)
        val context = PromptBuilder.buildVictimsContext(listOf(v), ChatScope.All)
        assertTrue(context.contains("P2｜軽症"))
        assertFalse(context.contains("場所:"))
        assertFalse(context.contains("見た目:"))
        assertFalse(context.contains("歩行:"))
    }

    @Test
    fun 長文のメモは切り詰められる() {
        val longText = "あ".repeat(100)
        val v = victim(displayNo = 1, result = TriageResult.MINOR, note = PatientNote(feature = longText))
        val context = PromptBuilder.buildVictimsContext(listOf(v), ChatScope.All)
        assertTrue(context.contains("見た目:" + "あ".repeat(PromptBuilder.MAX_NOTE_LENGTH)))
        assertFalse(context.contains("あ".repeat(PromptBuilder.MAX_NOTE_LENGTH + 1)))
    }

    // ── スコープフィルタ ──────────────────────────────────────────────

    @Test
    fun 重症スコープは重症者のみ含む() {
        val victims = listOf(
            victim(1, TriageResult.MINOR),
            victim(2, TriageResult.SEVERE),
            victim(3, TriageResult.SEVERE),
            victim(4, TriageResult.MINOR),
        )
        val context = PromptBuilder.buildVictimsContext(victims, ChatScope.Severe)
        assertTrue(context.contains("[今の会話の対象] 重症（P2, P3）"))
        assertTrue(context.contains("P2｜重症"))
        assertTrue(context.contains("P3｜重症"))
        assertFalse(context.contains("P1｜"))
        assertFalse(context.contains("P4｜"))
    }

    @Test
    fun 軽症スコープは軽症者のみ含む() {
        val victims = listOf(
            victim(1, TriageResult.MINOR),
            victim(2, TriageResult.SEVERE),
        )
        val context = PromptBuilder.buildVictimsContext(victims, ChatScope.Minor)
        assertTrue(context.contains("[今の会話の対象] 軽症（P1）"))
        assertFalse(context.contains("P2｜"))
    }

    @Test
    fun 全員スコープは全員含む() {
        val victims = listOf(
            victim(1, TriageResult.MINOR),
            victim(2, TriageResult.SEVERE),
        )
        val context = PromptBuilder.buildVictimsContext(victims, ChatScope.All)
        assertTrue(context.contains("[今の会話の対象] 全員（P1, P2）"))
    }

    @Test
    fun 対象者なしは空文字() {
        assertEquals("", PromptBuilder.buildVictimsContext(emptyList(), ChatScope.All))
    }

    @Test
    fun スコープ該当者なしは空文字() {
        val victims = listOf(victim(1, TriageResult.MINOR))
        assertEquals("", PromptBuilder.buildVictimsContext(victims, ChatScope.Severe))
    }

    @Test
    fun 個別選択スコープは指定した1人のみ含む() {
        val victims = listOf(
            victim(1, TriageResult.MINOR),
            victim(2, TriageResult.SEVERE),
            victim(3, TriageResult.SEVERE),
        )
        val context = PromptBuilder.buildVictimsContext(victims, ChatScope.Custom(setOf(2)))
        assertTrue(context.contains("[今の会話の対象] 個別（P2）"))
        assertTrue(context.contains("P2｜重症"))
        assertFalse(context.contains("P1｜"))
        assertFalse(context.contains("P3｜"))
    }

    @Test
    fun 個別選択スコープは指定した複数人のみ含む() {
        val victims = listOf(
            victim(1, TriageResult.MINOR),
            victim(2, TriageResult.SEVERE),
            victim(3, TriageResult.SEVERE),
            victim(4, TriageResult.MINOR),
        )
        val context = PromptBuilder.buildVictimsContext(victims, ChatScope.Custom(setOf(1, 3)))
        assertTrue(context.contains("[今の会話の対象] 個別（P1, P3）"))
        assertTrue(context.contains("P1｜軽症"))
        assertTrue(context.contains("P3｜重症"))
        assertFalse(context.contains("P2｜"))
        assertFalse(context.contains("P4｜"))
    }

    @Test
    fun 個別選択で存在しないdisplayNoは無視される() {
        val victims = listOf(victim(1, TriageResult.MINOR))
        assertEquals("", PromptBuilder.buildVictimsContext(victims, ChatScope.Custom(setOf(99))))
    }

    @Test
    fun 個別選択も上限超過時は省略ラベルが付く() {
        val victims = (1..11).map { victim(it, TriageResult.MINOR, recordedAt = it.toLong()) }
        val context = PromptBuilder.buildVictimsContext(
            victims, ChatScope.Custom((1..11).toSet()), maxVictims = 10
        )
        assertTrue(context.contains("※対象が多いため P1 の1人は省略"))
        assertFalse(context.contains("P1｜"))
    }

    // ── ラベル不変性（表示順とラベルの分離）──────────────────────────

    @Test
    fun 入力順を入れ替えてもP番号は変わらない() {
        val victims = listOf(
            victim(1, TriageResult.MINOR),
            victim(2, TriageResult.SEVERE),
            victim(3, TriageResult.SEVERE),
        )
        val original = PromptBuilder.buildVictimsContext(victims, ChatScope.Severe)
        val shuffled = PromptBuilder.buildVictimsContext(victims.reversed(), ChatScope.Severe)
        assertEquals(original, shuffled)
        assertTrue(original.contains("重症（P2, P3）"))
    }

    // ── 上限 ──────────────────────────────────────────────────────────

    @Test
    fun 上限超過時は重症優先で採用し省略者を明示する() {
        val victims = listOf(
            victim(1, TriageResult.MINOR, recordedAt = 1L),
            victim(2, TriageResult.MINOR, recordedAt = 2L),
            victim(3, TriageResult.SEVERE, recordedAt = 3L),
            victim(4, TriageResult.SEVERE, recordedAt = 4L),
        )
        val context = PromptBuilder.buildVictimsContext(victims, ChatScope.All, maxVictims = 3)
        // 重症2人＋新しい軽症1人（P2）が採用され、P1 が省略される
        assertTrue(context.contains("P3｜重症"))
        assertTrue(context.contains("P4｜重症"))
        assertTrue(context.contains("P2｜軽症"))
        assertFalse(context.contains("P1｜"))
        assertTrue(context.contains("※対象が多いため P1 の1人は省略"))
    }
}

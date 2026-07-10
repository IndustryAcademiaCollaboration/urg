package com.urg.edge

import com.urg.edge.llm.LlmConfig
import com.urg.edge.llm.LlmEngine
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// 地図遷移の確認フロー（pendingMapNavVictimId の遷移）に絞った ChatViewModel のテスト。
// Dispatchers.Unconfined を渡すことで viewModelScope.launch(ioDispatcher) が同期実行される。
class ChatViewModelMapNavTest {

    private class FakeLlmEngine : LlmEngine {
        override suspend fun generateStream(prompt: String, onToken: (partial: String, done: Boolean) -> Unit) {
            onToken("応答", true)
        }
        override fun close() {}
    }

    private class FakeRetriever : KnowledgeRetriever {
        override fun retrieve(query: String, topK: Int): List<KnowledgeChunk> = emptyList()
        override fun close() {}
    }

    private class FakeRepository(initial: List<VictimRecord>) : TriageSessionRepository {
        private val victims = initial.toMutableList()
        override fun startSession(latitude: Double?, longitude: Double?): TriageSession =
            TriageSession("session-1", latitude, longitude, 0L)
        override fun getLatestSession(): TriageSession? = null
        override fun saveVictim(victim: VictimRecord): VictimRecord {
            victims += victim
            return victim
        }
        override fun getVictimsBySession(sessionId: String): List<VictimRecord> = victims.toList()
        override fun updateVictimNote(victimId: String, note: PatientNote) {}
        override fun deleteVictim(victimId: String) {
            victims.removeAll { it.id == victimId }
        }
    }

    private fun victim(
        displayNo: Int,
        latitude: Double? = 35.0,
        longitude: Double? = 139.0,
        input: TriageInput = TriageInput(),
        result: TriageResult = TriageResult.SEVERE,
    ): VictimRecord = VictimRecord(
        id = "id-$displayNo",
        sessionId = "session-1",
        displayNo = displayNo,
        triageInput = input,
        result = result,
        actionPlan = StartRuleEngine.decideActions(result, input),
        latitude = latitude,
        longitude = longitude,
        recordedAt = displayNo.toLong(),
    )

    private fun createViewModel(victims: List<VictimRecord>): ChatViewModel {
        val vm = ChatViewModel(Dispatchers.Unconfined)
        vm.setLlmEngine(FakeLlmEngine(), LlmConfig())
        vm.setRetriever(FakeRetriever())
        vm.setRepository(FakeRepository(victims), TriageSession("session-1", null, null, 0L))
        return vm
    }

    private fun send(vm: ChatViewModel, text: String) {
        vm.updatePrompt(text)
        vm.onSendClick()
    }

    // ── 確認フローの開始 ───────────────────────────────────────────────

    @Test
    fun 場所質問でpendingがセットされ確認メッセージが出る() {
        val vm = createViewModel(listOf(victim(1)))
        send(vm, "P1はどこ？")
        val state = vm.uiState.value
        assertEquals("id-1", state.pendingMapNavVictimId)
        assertEquals(Strings.mapNavConfirm(1), state.messages.last().text)
        // 確認のやり取りはLLM履歴（CHAT）に混入しない
        assertTrue(state.messages.all { it.type == MessageType.MAP_NAV })
    }

    @Test
    fun 位置情報なしの対象者は定型応答でpendingはセットされない() {
        val vm = createViewModel(listOf(victim(1, latitude = null, longitude = null)))
        send(vm, "P1はどこ？")
        val state = vm.uiState.value
        assertNull(state.pendingMapNavVictimId)
        assertEquals(Strings.mapNavNoLocation(1), state.messages.last().text)
    }

    // ── pending の解除漏れ防止 ─────────────────────────────────────────

    @Test
    fun 確認待ちのまま別の質問を送るとpendingが解除される() {
        val vm = createViewModel(listOf(victim(1)))
        send(vm, "P1はどこ？")
        assertEquals("id-1", vm.uiState.value.pendingMapNavVictimId)

        // ボタンを押さずに無関係な質問を送信（通常LLM経路に流れる）
        send(vm, "止血の方法は？")
        assertNull(vm.uiState.value.pendingMapNavVictimId)
    }

    @Test
    fun 確認待ちのまま位置情報なしの対象者に質問してもpendingが解除される() {
        val vm = createViewModel(listOf(victim(1), victim(2, latitude = null, longitude = null)))
        send(vm, "P1はどこ？")
        assertEquals("id-1", vm.uiState.value.pendingMapNavVictimId)

        send(vm, "P2はどこ？")
        assertNull(vm.uiState.value.pendingMapNavVictimId)
    }

    // ── はい/いいえ応答 ────────────────────────────────────────────────

    @Test
    fun answerMapNavYesは対象者が存在すればvictimIdを返す() {
        val vm = createViewModel(listOf(victim(1)))
        send(vm, "P1はどこ？")
        assertEquals("id-1", vm.answerMapNavYes())
        assertNull(vm.uiState.value.pendingMapNavVictimId)
    }

    @Test
    fun answerMapNavYesは削除済み対象者ならnullを返しpendingを解除する() {
        val vm = createViewModel(listOf(victim(1)))
        send(vm, "P1はどこ？")
        assertEquals("id-1", vm.uiState.value.pendingMapNavVictimId)

        // 確認表示中に対象者が削除されたケース
        vm.deleteVictim("id-1")
        assertNull(vm.answerMapNavYes())
        val state = vm.uiState.value
        assertNull(state.pendingMapNavVictimId)
        assertEquals(Strings.MAP_NAV_VICTIM_NOT_FOUND, state.messages.last().text)
    }

    @Test
    fun answerMapNavNoはpendingを解除し定型応答を出す() {
        val vm = createViewModel(listOf(victim(1)))
        send(vm, "P1はどこ？")
        vm.answerMapNavNo()
        val state = vm.uiState.value
        assertNull(state.pendingMapNavVictimId)
        assertEquals(Strings.MAP_NAV_DECLINED, state.messages.last().text)
        assertEquals(MessageType.MAP_NAV, state.messages.last().type)
    }
}

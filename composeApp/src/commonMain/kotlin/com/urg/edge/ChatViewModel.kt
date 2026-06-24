package com.urg.edge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urg.edge.llm.LlmConfig
import com.urg.edge.llm.LlmEngine
import com.urg.edge.stt.SttEngine
import com.urg.edge.tts.TtsEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatViewModel(
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    @Suppress("unused")
    constructor() : this(Dispatchers.Default)

    private var llmEngine: LlmEngine? = null
    private var retriever: KnowledgeRetriever? = null
    private var sttEngine: SttEngine? = null
    private var ttsEngine: TtsEngine? = null
    private var config: LlmConfig = LlmConfig()
    private var repository: TriageSessionRepository? = null
    private var currentSession: TriageSession? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _victims = MutableStateFlow<List<VictimRecord>>(emptyList())
    val victims: StateFlow<List<VictimRecord>> = _victims.asStateFlow()

    private val triageController = TriageController()

    fun setLlmEngine(engine: LlmEngine, config: LlmConfig) {
        llmEngine = engine
        this.config = config
    }
    fun setRetriever(r: KnowledgeRetriever) { retriever = r }
    fun setSttEngine(engine: SttEngine) { sttEngine = engine }
    fun setTtsEngine(engine: TtsEngine) { ttsEngine = engine }
    fun setRepository(repo: TriageSessionRepository, session: TriageSession) {
        repository = repo
        currentSession = session
        refreshVictims()
    }

    private fun refreshVictims() {
        val repo = repository ?: return
        viewModelScope.launch(ioDispatcher) {
            _victims.value = repo.getVictimsByPriority()
        }
    }

    fun updateVictimNote(victimId: String, note: PatientNote) {
        val repo = repository ?: return
        viewModelScope.launch(ioDispatcher) {
            repo.updateVictimNote(victimId, note)
            _victims.value = repo.getVictimsByPriority()
        }
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    fun saveVictimFromFlow(result: TriageResult, input: TriageInput) {
        val repo = repository ?: return
        val session = currentSession ?: return
        val plan = StartRuleEngine.decideActions(result, input)
        val victim = VictimRecord(
            id = Uuid.random().toString(),
            sessionId = session.id,
            triageInput = input,
            result = result,
            actionPlan = plan,
            recordedAt = Clock.System.now().toEpochMilliseconds()
        )
        viewModelScope.launch(ioDispatcher) {
            repo.saveVictim(victim)
            _victims.value = repo.getVictimsByPriority()
        }
    }

    fun setListening(listening: Boolean) {
        _uiState.update { it.copy(isListening = listening) }
    }

    fun recognizeFromSamples(samples: FloatArray) {
        val engine = sttEngine ?: run {
            addSystemMessage(Strings.ERROR_STT_NOT_INITIALIZED)
            return
        }
        println("STT_RECOGNIZE: enter samples=${samples.size}")
        if (samples.isEmpty()) return
        // モデルが必要とする最小サンプル数 = 0.5秒 @ 16kHz
        val minSamples = 8000
        if (samples.size < minSamples) {
            println("STT_RECOGNIZE: skipped (too short: ${samples.size} < $minSamples)")
            return
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                val text = engine.recognize(samples)
                println("STT_RECOGNIZE: result text='$text'")
                if (text.isNotBlank()) {
                    _uiState.update { it.copy(promptText = text) }
                }
            } catch (e: Exception) {
                println("STT_RECOGNIZE: error ${e::class.simpleName}: ${e.message}")
                addSystemMessage("${Strings.ERROR_RECOGNITION_FAILED}${e.message}")
            }
        }
    }

    fun addSystemMessage(text: String) {
        appendMessage(Message("assistant", text, MessageType.SYSTEM))
    }

    fun updateLastSystemMessage(text: String) {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val idx = messages.indexOfLast { it.type == MessageType.SYSTEM }
            if (idx >= 0) messages[idx] = messages[idx].copy(text = text)
            state.copy(messages = messages)
        }
    }

    fun updatePrompt(text: String) {
        _uiState.update { it.copy(promptText = text) }
    }

    fun onSendClick() {
        if (triageController.isActive) handleTriageResponse() else generateResponse()
    }

    fun startTriage() {
        _uiState.update { it.copy(promptText = "", showTriageButtons = false) }
        appendMessage(Message("assistant", triageController.start(), MessageType.TRIAGE))
        _uiState.update { it.copy(showTriageButtons = true) }
    }

    fun answerTriageYes() {
        _uiState.update { it.copy(promptText = Strings.BUTTON_YES) }
        handleTriageResponse()
    }

    fun answerTriageNo() {
        _uiState.update { it.copy(promptText = Strings.BUTTON_NO) }
        handleTriageResponse()
    }

    private fun handleTriageResponse() {
        val text = _uiState.value.promptText.trim()
        if (text.isBlank()) return

        appendMessage(Message("user", text, MessageType.TRIAGE))
        _uiState.update { it.copy(promptText = "") }

        val result = triageController.handleAnswer(text)

        if (result is TriageHandleResult.InvalidAnswer) {
            val currentStep = triageController.step
            if (llmEngine != null && currentStep != null) {
                classifyWithLlm(text, currentStep, fallback = result)
                return
            }
        }

        applyTriageResult(result)
    }

    private fun classifyWithLlm(
        userInput: String,
        step: TriageStep,
        fallback: TriageHandleResult.InvalidAnswer
    ) {
        val engine = llmEngine ?: run { applyTriageResult(fallback); return }
        val question = StartRuleEngine.stepQuestions[step] ?: run { applyTriageResult(fallback); return }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch(ioDispatcher) {
            try {
                val instruction = PromptBuilder.buildYesNoClassificationInstruction(userInput, question)
                val prompt = config.chatTemplate.formatInstructionPrompt(instruction)
                val accumulated = StringBuilder()

                engine.generateStream(prompt) { partial, done ->
                    accumulated.append(partial)
                    if (done) {
                        _uiState.update { it.copy(isLoading = false) }
                        println("LLM_CLASSIFY: output='${accumulated}'")
                        val classified = StartRuleEngine.parseLlmYesNo(accumulated.toString())
                        if (classified != null) {
                            val classifiedText = if (classified) "はい" else "いいえ"
                            appendMessage(Message("assistant", "（AIが「$classifiedText」と解釈しました）", MessageType.TRIAGE))
                            val newResult = triageController.handleAnswer(classifiedText)
                            applyTriageResult(newResult)
                        } else {
                            appendMessage(Message("assistant", "（AIが判断できませんでした）", MessageType.TRIAGE))
                            applyTriageResult(fallback)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                applyTriageResult(fallback)
            }
        }
    }

    private fun applyTriageResult(result: TriageHandleResult) {
        when (result) {
            is TriageHandleResult.Ignored -> Unit
            is TriageHandleResult.InvalidAnswer -> {
                appendMessage(Message("assistant", result.message, MessageType.TRIAGE))
                _uiState.update { it.copy(showTriageButtons = true) }
            }
            is TriageHandleResult.SafetyFailed -> {
                appendMessage(Message("assistant", result.message, MessageType.TRIAGE))
                _uiState.update { it.copy(showTriageButtons = false) }
            }
            is TriageHandleResult.NextQuestion -> {
                appendMessage(Message("assistant", result.question, MessageType.TRIAGE))
                _uiState.update { it.copy(showTriageButtons = true) }
            }
            is TriageHandleResult.Done -> {
                appendMessage(Message("assistant", result.guidanceMessage, MessageType.TRIAGE))
                _uiState.update { it.copy(showTriageButtons = false) }
                saveVictimFromFlow(result.result, result.input)
                generateTriageGuidance(result.actionPlan)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun generateTriageGuidance(
        actionPlan: TriageActionPlan,
        appendToChat: Boolean = false
    ) {
        val engine = llmEngine ?: return

        _uiState.update {
            it.copy(
                isLoading = true,
                streamingText = ""
            )
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                val chunks = retriever?.retrieve(actionPlan.actions.joinToString(" "), topK = 2)
                    ?: emptyList()

                val supplementText = if (chunks.isNotEmpty()) {
                    "\n\n[補足知識]\n" + chunks.joinToString("\n") { "・${it.title}: ${it.text}" }
                } else {
                    ""
                }

                val systemPrompt = PromptBuilder.buildTriageSystemPrompt()
                val userPrompt = PromptBuilder.buildTriageUserPrompt(actionPlan, supplementText)
                val prompt = config.chatTemplate.formatSinglePrompt(systemPrompt, userPrompt)
                val accumulated = StringBuilder()
                var lastUpdateTime = 0L

                engine.generateStream(prompt) { partial, done ->
                    accumulated.append(partial)

                    val now = Clock.System.now().toEpochMilliseconds()
                    if (done || now - lastUpdateTime > 100) {
                        lastUpdateTime = now
                        _uiState.update {
                            it.copy(streamingText = accumulated.toString())
                        }
                    }

                    if (done) {
                        val finalText = accumulated.toString()

                        if (appendToChat) {
                            appendMessage(Message("assistant", finalText))
                            _uiState.update {
                                it.copy(
                                    streamingText = "",
                                    isLoading = false
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    streamingText = finalText,
                                    isLoading = false
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        streamingText = "案内文の生成に失敗しました。",
                        isLoading = false
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun generateTriageFlowGuidanceFromReachedResult(reachedResult: String) {
        val engine = llmEngine ?: run {
            _uiState.update {
                it.copy(
                    triageFlowText = "LLMがまだ準備できていません。",
                    isTriageFlowLoading = false
                )
            }
            return
        }

        val actionPlan = buildActionPlanFromReachedResult(reachedResult)

        _uiState.update {
            it.copy(
                triageFlowText = "",
                isTriageFlowLoading = true
            )
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                val chunks = retriever?.retrieve(actionPlan.actions.joinToString(" "), topK = 2)
                    ?: emptyList()

                val supplementText = if (chunks.isNotEmpty()) {
                    "\n\n[補足知識]\n" + chunks.joinToString("\n") { "・${it.title}: ${it.text}" }
                } else {
                    ""
                }

                val systemPrompt = PromptBuilder.buildTriageSystemPrompt()
                val userPrompt = PromptBuilder.buildTriageUserPrompt(actionPlan, supplementText)
                val prompt = config.chatTemplate.formatSinglePrompt(systemPrompt, userPrompt)

                val accumulated = StringBuilder()
                var lastUpdateTime = 0L

                engine.generateStream(prompt) { partial, done ->
                    accumulated.append(partial)

                    val now = Clock.System.now().toEpochMilliseconds()
                    if (done || now - lastUpdateTime > 100) {
                        lastUpdateTime = now
                        _uiState.update {
                            it.copy(triageFlowText = accumulated.toString())
                        }
                    }

                    if (done) {
                        _uiState.update {
                            it.copy(
                                triageFlowText = accumulated.toString(),
                                isTriageFlowLoading = false
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        triageFlowText = "案内文の生成に失敗しました。",
                        isTriageFlowLoading = false
                    )
                }
                println("TRIAGE_FLOW_LLM_FAILED: ${t::class.simpleName}: ${t.message}")
            }
        }
    }

    fun generateTriageGuidanceFromReachedResult(reachedResult: String) {
        val actionPlan = buildActionPlanFromReachedResult(reachedResult)
        generateTriageGuidance(
            actionPlan = actionPlan,
            appendToChat = false
        )
    }

    //トリアージの結果からアクションリストを作成する箇所。この中身を変えるとLLMの出力が変わります。
    private fun buildActionPlanFromReachedResult(reachedResult: String): TriageActionPlan {
        return when (reachedResult) {
            "danger" -> TriageActionPlan(
                destination = "安全な場所",
                safetyFirst = listOf("周囲の安全を確認する"),
                actions = listOf("危険な場所から離れる", "安全な場所へ避難する"),
                forbiddenActions = listOf("危険な場所で救助を続けない")
            )

            "minor" -> TriageActionPlan(
                destination = "救護所",
                safetyFirst = listOf("周囲の安全を確認する"),
                actions = listOf("自力で救護所へ向かう", "症状の変化を観察する"),
                forbiddenActions = listOf("無理に走らない")
            )

            "severe_airway" -> TriageActionPlan(
                destination = "救護所",
                safetyFirst = listOf("周囲の安全を確認する"),
                actions = listOf("気道を確保する", "救助隊に知らせる"),
                forbiddenActions = listOf("一人で搬送しない", "首を大きく動かさない")
            )

            "severe_circ" -> TriageActionPlan(
                destination = "救護所",
                safetyFirst = listOf("周囲の安全を確認する"),
                actions = listOf("出血部位を圧迫する", "圧迫を続ける", "救助隊に知らせる"),
                forbiddenActions = listOf("止血せずに動かさない", "一人で搬送しない")
            )

            "severe_cons" -> TriageActionPlan(
                destination = "救護所",
                safetyFirst = listOf("周囲の安全を確認する"),
                actions = listOf("呼吸を確認する", "救助隊に知らせる"),
                forbiddenActions = listOf("一人で搬送しない", "首を大きく動かさない")
            )

            "severe_injury" -> TriageActionPlan(
                destination = "救護所",
                safetyFirst = listOf("周囲の安全を確認する"),
                actions = listOf("出血箇所を圧迫する", "安静にする", "救助隊に知らせる"),
                forbiddenActions = listOf("無理に動かさない", "一人で搬送しない")
            )

            else -> TriageActionPlan(
                destination = "救護所",
                safetyFirst = listOf("周囲の安全を確認する"),
                actions = listOf("救助隊に知らせる"),
                forbiddenActions = listOf("無理に動かさない")
            )
        }
    }

    private fun generateResponse() {
        val text = _uiState.value.promptText.trim()
        if (text.isBlank() || _uiState.value.isLoading) return

        val engine = llmEngine ?: run {
            addSystemMessage(Strings.ERROR_LLM_NOT_INITIALIZED)
            return
        }
        val r = retriever ?: run {
            addSystemMessage(Strings.RETRIEVER_INITIALIZING)
            return
        }

        appendMessage(Message("user", text))
        _uiState.update { it.copy(promptText = "", isLoading = true) }

        val currentMessages = _uiState.value.messages.filter { it.type == MessageType.CHAT }.takeLast(8)

        viewModelScope.launch(ioDispatcher) {
            try {
                // 修正後
                val chunks = r.retrieve(text, topK = 3)

                val ragSection = if (chunks.isNotEmpty()) {
                    "\n\n[参考情報]\n" + chunks.joinToString("\n") {
                        val base = "・${it.title}: ${it.text}"
                        if (it.guidance != null) "$base\n  推奨対応: ${it.guidance}" else base
                    }
                } else ""
                val forbiddenList = StartRuleEngine.globalForbiddenSevere.joinToString("\n") { "- $it" }
                val triageContext = buildTriageContext()
                val systemPrompt = PromptBuilder.buildChatSystemPrompt(ragSection, forbiddenList, triageContext)
                val prompt = config.chatTemplate.formatChatPrompt(systemPrompt, currentMessages)
                //Log.d("LLM",prompt)

                val accumulated = StringBuilder()
                engine.generateStream(prompt) { partial, done ->
                    accumulated.append(partial)
                    _uiState.update { it.copy(streamingText = accumulated.toString()) }
                    if (done) {
                        // appendMessage 経由で speak される（appendMessage 内で発火）。
                        appendMessage(Message("assistant", accumulated.toString()))
                        _uiState.update { it.copy(streamingText = "", isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                appendMessage(Message("assistant", "${Strings.ERROR_PREFIX}${e.message}", MessageType.SYSTEM))
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun buildTriageContext(): String {
        val result = triageController.lastResult ?: return ""
        val plan = triageController.lastActionPlan ?: return ""
        val label = if (result == TriageResult.MINOR) "軽症" else "重症"
        val actions = (plan.safetyFirst + plan.actions).joinToString("、")
        return "\n\n[トリアージ済み情報]\n判定：$label\n搬送先：${plan.destination}\n確認済み行動：$actions"
    }

    private fun speak(text: String) {
        val engine = ttsEngine ?: return
        viewModelScope.launch(ioDispatcher) {
            try {
                engine.speak(text)
            } catch (e: Exception) {
                println("TTS_SPEAK: error ${e::class.simpleName}: ${e.message}")
            }
        }
    }

    private fun appendMessage(message: Message) {
        _uiState.update { it.copy(messages = it.messages + message) }
        // アプリ側が提示する文字を漏れなく読み上げる。
        // - role == "assistant"：トリアージ質問・判定、不正回答リトライ、LLM応答 等
        // - type != SYSTEM    ：初期化完了/エラー等の技術通知は読まない
        // ユーザー入力（role == "user"）は対象外。
        if (message.role == "assistant" && message.type != MessageType.SYSTEM) {
            speak(message.text)
        }
    }

    override fun onCleared() {
        llmEngine?.close()
        retriever?.close()
        sttEngine?.close()
        ttsEngine?.close()
        super.onCleared()
    }
}

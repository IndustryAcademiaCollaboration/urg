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

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val triageController = TriageController()

    fun setLlmEngine(engine: LlmEngine, config: LlmConfig) {
        llmEngine = engine
        this.config = config
    }
    fun setRetriever(r: KnowledgeRetriever) { retriever = r }
    fun setSttEngine(engine: SttEngine) { sttEngine = engine }
    fun setTtsEngine(engine: TtsEngine) { ttsEngine = engine }

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

    fun updatePrompt(text: String) {
        _uiState.update { it.copy(promptText = text) }
    }

    fun onSendClick() {
        if (triageController.isActive) handleTriageResponse() else generateResponse()
    }

    fun startTriage() {
        _uiState.update { it.copy(promptText = "", showTriageButtons = false) }
        appendMessage(Message("assistant", triageController.start(), MessageType.TRIAGE))
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
                        val classified = StartRuleEngine.parseAnswer(accumulated.toString())
                        if (classified != null) {
                            val classifiedText = if (classified) "はい" else "いいえ"
                            appendMessage(Message("assistant", "（AIが「$classifiedText」と解釈しました）", MessageType.TRIAGE))
                            val newResult = triageController.handleAnswer(classifiedText)
                            applyTriageResult(newResult)
                        } else {
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
                if (result.showButtons) _uiState.update { it.copy(showTriageButtons = true) }
            }
            is TriageHandleResult.SafetyFailed -> {
                appendMessage(Message("assistant", result.message, MessageType.TRIAGE))
                _uiState.update { it.copy(showTriageButtons = false) }
            }
            is TriageHandleResult.NextQuestion -> {
                appendMessage(Message("assistant", result.question, MessageType.TRIAGE))
                _uiState.update { it.copy(showTriageButtons = false) }
            }
            is TriageHandleResult.Done -> {
                appendMessage(Message("assistant", result.guidanceMessage, MessageType.TRIAGE))
                _uiState.update { it.copy(showTriageButtons = false) }
                generateTriageGuidance(result.actionPlan)
            }
        }
    }

    private fun generateTriageGuidance(actionPlan: TriageActionPlan) {
        val engine = llmEngine ?: return
        val r = retriever ?: return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch(ioDispatcher) {
            try {
                val chunks = r.retrieve(actionPlan.actions.joinToString(" "), topK = 2)
                val supplementText = if (chunks.isNotEmpty()) {
                    "\n\n[補足知識]\n" + chunks.joinToString("\n") { "・${it.title}: ${it.text}" }
                } else ""

                val systemPrompt = PromptBuilder.buildTriageSystemPrompt()
                val userPrompt = PromptBuilder.buildTriageUserPrompt(actionPlan, supplementText)
                val prompt = config.chatTemplate.formatSinglePrompt(systemPrompt, userPrompt)
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
                _uiState.update { it.copy(isLoading = false) }
            }
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
                val chunks = r.retrieve(text, topK = 3)

                if (chunks.isEmpty()) {
                    appendMessage(Message("assistant", Strings.NO_RELEVANT_INFO))
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val ragSection = "\n\n[参考情報]\n" + chunks.joinToString("\n") { "・${it.title}: ${it.text}" }
                val forbiddenList = StartRuleEngine.globalForbiddenSevere.joinToString("\n") { "- $it" }
                val triageContext = buildTriageContext()
                val systemPrompt = PromptBuilder.buildChatSystemPrompt(ragSection, forbiddenList, triageContext)
                val prompt = config.chatTemplate.formatChatPrompt(systemPrompt, currentMessages)

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

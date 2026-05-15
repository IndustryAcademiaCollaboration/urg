package com.urg.edge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urg.edge.llm.LlmConfig
import com.urg.edge.llm.LlmEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private var llmEngine: LlmEngine? = null
    private var retriever: KnowledgeRetriever? = null
    private var config: LlmConfig = LlmConfig()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _promptText = MutableStateFlow("")
    val promptText: StateFlow<String> = _promptText.asStateFlow()

    private val triageController = TriageController()

    fun setLlmEngine(engine: LlmEngine, config: LlmConfig) {
        llmEngine = engine
        this.config = config
    }
    fun setRetriever(r: KnowledgeRetriever) { retriever = r }

    fun addSystemMessage(text: String) {
        appendMessage(Message("assistant", text, MessageType.SYSTEM))
    }

    fun updatePrompt(text: String) { _promptText.value = text }

    fun onSendClick() {
        if (triageController.isActive) handleTriageResponse() else generateResponse()
    }

    fun startTriage() {
        _promptText.value = ""
        appendMessage(Message("assistant", triageController.start(), MessageType.TRIAGE))
    }

    private fun handleTriageResponse() {
        val text = _promptText.value.trim()
        if (text.isBlank()) return

        appendMessage(Message("user", text, MessageType.TRIAGE))
        _promptText.value = ""

        when (val result = triageController.handleAnswer(text)) {
            is TriageHandleResult.Ignored -> Unit
            is TriageHandleResult.InvalidAnswer -> appendMessage(Message("assistant", result.message, MessageType.TRIAGE))
            is TriageHandleResult.SafetyFailed -> appendMessage(Message("assistant", result.message, MessageType.TRIAGE))
            is TriageHandleResult.NextQuestion -> appendMessage(Message("assistant", result.question, MessageType.TRIAGE))
            is TriageHandleResult.Done -> {
                appendMessage(Message("assistant", result.guidanceMessage, MessageType.TRIAGE))
                generateTriageGuidance(result.actionPlan)
            }
        }
    }

    private fun generateTriageGuidance(actionPlan: TriageActionPlan) {
        val engine = llmEngine ?: return
        val r = retriever ?: return
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.Default) {
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
                    _streamingText.value = accumulated.toString()
                    if (done) {
                        appendMessage(Message("assistant", accumulated.toString()))
                        _streamingText.value = ""
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    private fun generateResponse() {
        val text = _promptText.value.trim()
        if (text.isBlank() || _isLoading.value) return

        val engine = llmEngine ?: run {
            appendMessage(Message("assistant", "ERROR: LLM is not initialized", MessageType.SYSTEM))
            return
        }
        val r = retriever ?: run {
            appendMessage(Message("assistant", "知識ベースの初期化中です。しばらくお待ちください。", MessageType.SYSTEM))
            return
        }

        appendMessage(Message("user", text))
        _promptText.value = ""
        _isLoading.value = true

        val currentMessages = _messages.value.filter { it.type == MessageType.CHAT }.takeLast(8)

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val chunks = r.retrieve(currentMessages.last().text, topK = 3)

                if (chunks.isEmpty()) {
                    appendMessage(Message("assistant", "申し訳ありませんが、その状況に関する情報を持ち合わせていません。近くの救護所または避難所でご確認ください。"))
                    _isLoading.value = false
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
                    _streamingText.value = accumulated.toString()
                    if (done) {
                        appendMessage(Message("assistant", accumulated.toString()))
                        _streamingText.value = ""
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                appendMessage(Message("assistant", "ERROR: ${e.message}", MessageType.SYSTEM))
                _isLoading.value = false
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

    private fun appendMessage(message: Message) {
        _messages.update { it + message }
    }

    override fun onCleared() {
        llmEngine?.close()
        retriever?.close()
        super.onCleared()
    }
}

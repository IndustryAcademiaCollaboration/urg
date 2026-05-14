package com.urg.edge

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private var llmInference: LlmInference? = null
    private var embeddingRetriever: EmbeddingRetriever? = null

    private var promptText by mutableStateOf("")
    private var messages by mutableStateOf(listOf<Message>())
    private var isLoading by mutableStateOf(false)

    private var triageStep: TriageStep? = null
    private var triageInput = TriageInput()

    private fun copyModelToInternalStorage(): File {
        val outFile = File(filesDir, "gemma3-1b-it-int4.task")

        if (!outFile.exists()) {
            assets.open("models/gemma3-1b-it-int4.task").use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return outFile
    }

    private fun startTriage() {
        triageInput = TriageInput()
        triageStep = TriageStep.WALK
        promptText = ""
        messages = messages + Message("assistant", "STARTトリアージを開始します。\n\n${StartRuleEngine.stepQuestions[TriageStep.WALK]!!}")
    }

    private fun handleTriageResponse() {
        val step = triageStep ?: return
        if (step == TriageStep.DONE) return
        if (promptText.isBlank()) return

        val answer = StartRuleEngine.parseAnswer(promptText)
        messages = messages + Message("user", promptText)
        promptText = ""

        if (answer == null) {
            messages = messages + Message("assistant", "「はい」か「いいえ」でお答えください。\n${StartRuleEngine.stepQuestions[step]!!}")
            return
        }

        triageInput = StartRuleEngine.applyAnswer(triageInput, step, answer)

        // 歩行可能 → 軽症確定、残りの質問をスキップ
        if (step == TriageStep.WALK && answer == true) {
            triageStep = TriageStep.DONE
            val result = StartRuleEngine.evaluate(triageInput)
            messages = messages + Message("assistant", StartRuleEngine.toGuidance(result))
            generateTriageGuidance(result, triageInput)
            return
        }

        val nextStep = StartRuleEngine.nextStep(step)
        triageStep = nextStep

        if (nextStep == TriageStep.DONE) {
            val result = StartRuleEngine.evaluate(triageInput)
            messages = messages + Message("assistant", StartRuleEngine.toGuidance(result))
            generateTriageGuidance(result, triageInput)
        } else {
            messages = messages + Message("assistant", StartRuleEngine.stepQuestions[nextStep]!!)
        }
    }

    private fun generateTriageGuidance(result: TriageResult, input: TriageInput) {
        isLoading = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inference = llmInference ?: run {
                    withContext(Dispatchers.Main) { isLoading = false }
                    return@launch
                }
                val retriever = embeddingRetriever ?: run {
                    withContext(Dispatchers.Main) { isLoading = false }
                    return@launch
                }

                // Rule Engine がアクションリストを確定
                val actionPlan = StartRuleEngine.decideActions(result, input)

                // RAG: アクションリストに関連する知識を言葉選びの補足として取得
                val ragQuery = actionPlan.actions.joinToString(" ")
                val retrievedChunks = retriever.retrieve(ragQuery, topK = 2)
                val supplementText = if (retrievedChunks.isNotEmpty()) {
                    "\n\n[補足知識]\n" + retrievedChunks.joinToString("\n") { "・${it.title}: ${it.text}" }
                } else ""

                val actionListText = actionPlan.actions
                    .mapIndexed { i, action -> "${i + 1}. $action" }
                    .joinToString("\n")

                val forbiddenText = if (actionPlan.forbiddenActions.isNotEmpty()) {
                    "\n\n[禁止行為]\n" + actionPlan.forbiddenActions.joinToString("\n") { "・$it" }
                } else ""

                val systemPrompt = """
あなたは災害時支援AIです。
必ず日本語で答えてください。

重要:
- 与えられた行動リスト以外を提案しない
- 搬送先を変更しない
- 医療診断をしない
- 新しい処置を追加しない
- 3〜5項目・各1文・短く
- 必ず落ち着いた口調で説明する
""".trimIndent()

                val userPrompt = """
以下の行動リストを、一般市民向けに、短く・落ち着いた口調で番号付きリストとして説明してください。
[補足知識]がある場合は、説明の言葉選びの参考にしてください。行動リスト以外の内容は追加しないでください。

[搬送方針]
${actionPlan.destination}

[行動リスト]
$actionListText
$forbiddenText
$supplementText
""".trimIndent()

                val fullPrompt = "<start_of_turn>user\n$systemPrompt<end_of_turn>\n<start_of_turn>user\n$userPrompt<end_of_turn>\n<start_of_turn>model\n"

                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(30)
                    .setTopP(0.9f)
                    .setTemperature(0.3f)
                    .build()

                val session = LlmInferenceSession.createFromOptions(inference, sessionOptions)
                val response = try {
                    session.addQueryChunk(fullPrompt)
                    session.generateResponse()
                } finally {
                    session.close()
                }

                withContext(Dispatchers.Main) {
                    messages = messages + Message("assistant", response)
                    isLoading = false
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false }
                Log.e("TRIAGE_LLM", "FAILED", e)
            }
        }
    }

    private fun generateResponse() {
        if (promptText.isBlank()) return
        if (isLoading) return

        messages = messages + Message("user", promptText)
        isLoading = true

        val currentMessages = messages

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val inference = llmInference ?: run {

                    withContext(Dispatchers.Main) {
                        messages = messages + Message("assistant", "ERROR: LLM is not initialized")
                        isLoading = false
                    }
                    return@launch
                }

                val retriever = embeddingRetriever ?: run {
                    withContext(Dispatchers.Main) {
                        messages = messages + Message("assistant", "知識ベースの初期化中です。しばらくお待ちください。")
                        isLoading = false
                    }
                    return@launch
                }
                val retrievedChunks = retriever.retrieve(currentMessages.last().text, topK = 3)

                if (retrievedChunks.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        messages = messages + Message("assistant", "申し訳ありませんが、その状況に関する情報を持ち合わせていません。近くの救護所または避難所でご確認ください。")
                        isLoading = false
                    }
                    return@launch
                }

                val ragSection = "\n\n[参考情報]\n" + retrievedChunks.joinToString("\n") { "・${it.title}: ${it.text}" }

                val systemPrompt = """
あなたは災害時支援AIです。
一般市民向けに行動支援を行います。
医療診断は行いません。
必ず日本語で答えてください。
Be concise and brief.
Do not repeat sentences.
具体的な時間の見積もりや数値は絶対に言及しないでください。
状況が不明な場合は、まず相手の状況を確認する質問をしてください。
出血の量が不明な場合は、必ず量を確認してから救護所または病院への誘導を行ってください。
状況に応じて、救護所または病院への誘導を行ってください。
助けようとする場合でも、周囲に二次災害の危険がある場合は、まず自分自身の避難を促してください。
[参考情報]の内容のみに基づいて回答してください。[参考情報]にない情報は回答しないでください。$ragSection
""".trimIndent()

                val historyPrompt = currentMessages.joinToString("") { msg ->
                    when (msg.role) {
                        "user"      -> "<start_of_turn>user\n${msg.text}<end_of_turn>\n"
                        "assistant" -> "<start_of_turn>model\n${msg.text}<end_of_turn>\n"
                        else        -> ""
                    }
                }
                val fullPrompt = "<start_of_turn>user\n$systemPrompt<end_of_turn>\n$historyPrompt<start_of_turn>model\n"

                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(30) // 次のトークン候補を確率上位K個に絞る設定。大きいと、文脈に無関係なワードが混在しやすくなる
                    .setTopP(0.9f) // 確率の累積が(引数 * 100)%に達するまでの候補のみに絞る設定
                    .setTemperature(0.3f) // 確率分布の「尖り」を調整: 高い:決定的で繰り返し 低い:多様、創造的、ハルシネーション増
                    .build()

                val session = LlmInferenceSession.createFromOptions(inference, sessionOptions)
                val response = try {
                    session.addQueryChunk(fullPrompt)
                    session.generateResponse()
                } finally {
                    session.close()
                }

                withContext(Dispatchers.Main) {
                    messages = messages + Message("assistant", response)
                    isLoading = false
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messages = messages + Message("assistant", "ERROR: ${e.message}")
                    isLoading = false
                }

                Log.e("LLM_RESPONSE", "FAILED", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        savedInstanceState?.let { state ->
            val roles = state.getStringArrayList("message_roles") ?: emptyList<String>()
            val texts = state.getStringArrayList("message_texts") ?: emptyList<String>()
            if (roles.size == texts.size) {
                messages = roles.zip(texts).map { (role, text) -> Message(role, text) }
            }
        }

        val modelFile = copyModelToInternalStorage()

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(512)
                .setMaxTopK(40)
                .build()

            llmInference = LlmInference.createFromOptions(this, options)

            Log.d("LLM_INIT", "SUCCESS")
        } catch (e: Exception) {
            messages = messages + Message("assistant", "ERROR: LLM initialization failed: ${e.message}")
            Log.e("LLM_INIT", "FAILED: ${e.message}", e)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val chunks = KnowledgeLoader.load(assets)
                withContext(Dispatchers.Main) {
                    messages = messages + Message("assistant", "[初期化] 知識ベース読み込み完了 (${chunks.size} chunks)")
                }
                embeddingRetriever = EmbeddingRetriever(this@MainActivity, chunks)
                withContext(Dispatchers.Main) {
                    messages = messages + Message("assistant", "[初期化] 準備完了")
                }
                Log.d("EMBEDDING_INIT", "SUCCESS")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messages = messages + Message("assistant", "[初期化エラー] ${e::class.simpleName}: ${e.message}")
                }
                Log.e("EMBEDDING_INIT", "FAILED: ${e.message}", e)
            }
        }

        setContent {
            App(
                prompt = promptText,
                messages = messages,
                isLoading = isLoading,
                onPromptChange = { promptText = it },
                onSendClick = {
                    val step = triageStep
                    if (step != null && step != TriageStep.DONE) {
                        handleTriageResponse()
                    } else {
                        generateResponse()
                    }
                },
                onTriageClick = { startTriage() }
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList("message_roles", ArrayList(messages.map { it.role }))
        outState.putStringArrayList("message_texts", ArrayList(messages.map { it.text }))
    }

    override fun onDestroy() {
        llmInference?.close()
        embeddingRetriever?.close()
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        prompt = "",
        messages = listOf(
            Message("user", "こんにちは"),
            Message("assistant", "Hello")
        ),
        isLoading = false,
        onPromptChange = {},
        onSendClick = {},
        onTriageClick = {}
    )
}
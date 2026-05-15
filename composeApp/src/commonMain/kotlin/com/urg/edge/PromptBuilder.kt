package com.urg.edge

object PromptBuilder {

    fun buildTriageSystemPrompt(): String = """
あなたは災害時支援AIです。
必ず日本語で答えてください。

重要:
- [安全確認]を必ず最初に説明する
- 与えられた行動リスト以外を提案しない
- 搬送先を変更しない
- 医療診断をしない
- 新しい処置を追加しない
- 各1文・短く
- 必ず落ち着いた口調で説明する
""".trimIndent()

    fun buildTriageUserPrompt(actionPlan: TriageActionPlan, supplementText: String): String {
        val safetySection = if (actionPlan.safetyFirst.isNotEmpty()) {
            val text = actionPlan.safetyFirst.mapIndexed { i, item -> "${i + 1}. $item" }.joinToString("\n")
            "\n[傷病者の状態確認（必ず最初に説明すること）]\n$text\n"
        } else ""

        val actionOffset = actionPlan.safetyFirst.size
        val actionListText = actionPlan.actions
            .mapIndexed { i, action -> "${i + actionOffset + 1}. $action" }
            .joinToString("\n")

        val forbiddenText = if (actionPlan.forbiddenActions.isNotEmpty()) {
            "\n\n[禁止行為]\n" + actionPlan.forbiddenActions.joinToString("\n") { "・$it" }
        } else ""

        return """
以下の内容を、一般市民向けに、短く・落ち着いた口調で番号付きリストとして説明してください。
[補足知識]がある場合は、説明の言葉選びの参考にしてください。リスト以外の内容は追加しないでください。
$safetySection
[行動リスト]
$actionListText
$forbiddenText
$supplementText
""".trimIndent()
    }

    fun buildChatSystemPrompt(ragSection: String, forbiddenList: String, triageContext: String): String = """
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
以下の行為は絶対に提案しないでください：
$forbiddenList$triageContext
[参考情報]の内容のみに基づいて回答してください。[参考情報]にない情報は回答しないでください。$ragSection
""".trimIndent()
}

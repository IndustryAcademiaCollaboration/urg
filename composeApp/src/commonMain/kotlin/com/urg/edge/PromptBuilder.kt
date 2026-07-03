package com.urg.edge

object PromptBuilder {

    fun buildTriageSystemPrompt(): String = """
You are a disaster response AI.
Please respond in Japanese only.

Important:
- Always explain Safety Check first
- Do not change the destination for transport
- Do not make medical diagnoses
- Do not add new procedures
- Do not add times, frequencies, or numerical values
- Do not repeat the same phrases
- Keep each sentence short
- Always explain in a calm tone
""".trimIndent()

    fun buildTriageUserPrompt(actionPlan: TriageActionPlan, supplementText: String): String {
        val safetySection = if (actionPlan.safetyFirst.isNotEmpty()) {
            val text = actionPlan.safetyFirst.distinct().joinToString("\n") { "・$it" }
            "\n[傷病者の状態確認（必ず最初に説明すること）]\n$text\n"
        } else ""

        val actionListText = actionPlan.actions.distinct().joinToString("\n") { "・$it" }

        val forbiddenText = if (actionPlan.forbiddenActions.isNotEmpty()) {
            "\n\n[禁止行為]\n" + actionPlan.forbiddenActions.distinct().joinToString("\n") { "・$it" }
        } else ""

        return """
Please create a set of instructions for the general public based on the following conditions:
Do not simply list the steps as a bulleted list.
Summarize the steps into a single, concise set of instructions.
Do not phrase the instructions as questions.
Do not use numbered lists.
Do not include “Yes” or “Understood” in the instructions.
Please pay attention to your use of conjunctions, such as “or” and “next,” to ensure your writing flows smoothly.
Please prioritize generating output that sounds like natural Japanese.

$safetySection
[Action List]
$actionListText
$forbiddenText

$supplementText


[Output Requirements]
Write exactly one sentence per item in the [Action List], in order.
Do not add any sentences beyond the [Action List] items.
Use varied Japanese sentence endings. Do not use 〜してください in every sentence.
Acceptable endings include: 〜しましょう、〜が重要です、〜を行います、〜する必要があります。

""".trimIndent()
    }


    fun buildYesNoClassificationInstruction(userInput: String, question: String): String = """
以下の質問に対するユーザーの回答が「はい」か「いいえ」かを判定してください。「YES」か「NO」のみ答えてください。

質問：$question
ユーザーの回答：「$userInput」
""".trimIndent()

    fun buildChatSystemPrompt(ragSection: String, forbiddenList: String, triageContext: String): String = """
あなたは災害時に救助活動を行う一般市民を支援するAIです。
傷病者（けが人・急病人）への対応方法を、一般市民に向けて行動案内します。
医療診断は行いません。
必ず日本語で答えてください。
簡潔に答えてください。同じ文を繰り返さないでください。
具体的な時間・回数・距離などの数値は言及しないでください。
状況が不明な場合は、まず相手の状況を確認する質問をしてください。
状況に応じて、医療機関や専門家への相談を促してください。
助けようとする場合でも、周囲に二次災害の危険がある場合は、まず自分自身の避難を促してください。
以下の行為は絶対に提案しないでください：
$forbiddenList$triageContext
[参考情報]がある場合はその内容を優先して回答してください。[参考情報]に該当する情報がない場合は、一般的な災害時の応急手当の知識で回答してください。$ragSection
""".trimIndent()
}
package com.urg.edge

data class TriageInput(
    val canWalk: Boolean? = null,
    val isBreathing: Boolean? = null,
    val hasCirculation: Boolean? = null,
    val isConscious: Boolean? = null
)

enum class TriageStep { SAFETY_CHECK, WALK, BREATHING, CIRCULATION, CONSCIOUSNESS, DONE }

enum class TriageResult { MINOR, SEVERE }

data class TriageActionPlan(
    val destination: String,
    val safetyFirst: List<String>,
    val actions: List<String>,
    val forbiddenActions: List<String>
)

object StartRuleEngine {

    // 重症時に傷病者に対して確認する項目
    val patientChecksSevere = listOf(
        "呼吸を確認する",
        "意識を確認する"
    )

    // 重症ならば状態に関わらず常に禁止する行為
    val globalForbiddenSevere = listOf(
        "首を動かす・ねじる",
        "一人だけで無理に搬送する",
        "止血せずに傷病者を動かす"
    )

    val stepQuestions = mapOf(
        TriageStep.SAFETY_CHECK to "【安全確認】周囲の状況は安全ですか？（はい / いいえ）",
        TriageStep.WALK to "【歩行確認】傷病者は自力で歩けますか？（はい / いいえ）",
        TriageStep.BREATHING to "【呼吸確認】普段通りの呼吸はありますか？（はい / いいえ）",
        TriageStep.CIRCULATION to "【循環確認】脈はありますか？（はい / いいえ）",
        TriageStep.CONSCIOUSNESS to "【意識確認】呼びかけに反応がありますか？（はい / いいえ）"
    )

    fun nextStep(current: TriageStep): TriageStep = when (current) {
        TriageStep.SAFETY_CHECK -> TriageStep.WALK
        TriageStep.WALK -> TriageStep.BREATHING
        TriageStep.BREATHING -> TriageStep.CIRCULATION
        TriageStep.CIRCULATION -> TriageStep.CONSCIOUSNESS
        TriageStep.CONSCIOUSNESS -> TriageStep.DONE
        TriageStep.DONE -> TriageStep.DONE
    }

    fun parseLlmYesNo(text: String): Boolean? {
        val t = text.trim()
        return when {
            t.contains("YES", ignoreCase = true) || t.contains("はい") -> true
            t.contains("NO", ignoreCase = true) || t.contains("いいえ") -> false
            else -> null
        }
    }

    fun parseAnswer(text: String): Boolean? {
        val t = text.trim()
        // 不確かな表現は先に null で除外して LLM フォールバックに流す
        if (t.contains("わからない") || t.contains("わかりません") || t.contains("不明")
            || t.contains("確認できない") || t.contains("確認できません")) return null
        return when {
            // 肯定 — "問題ない" は否定側の "ない" より先に評価する必要がある
            t.contains("はい") || t.contains("yes", ignoreCase = true) || t.contains("ok", ignoreCase = true)
            || t == "y" || t == "1"
            || t.contains("うん") || t.contains("ええ")
            || t.contains("できます") || t.contains("できる")
            || t.contains("歩けます") || t.contains("歩ける")
            || t.contains("大丈夫") || t.contains("問題ない")
            || t.contains("あります") || t.contains("反応あり") -> true

            // 否定
            t.contains("いいえ") || t.contains("no", ignoreCase = true) || t == "n" || t == "0"
            || t.contains("ありません") || t.contains("できません") || t.contains("できない")
            || t.contains("歩けません") || t.contains("歩けない")
            || t.contains("無理") || t.contains("なし") || t.contains("ない") -> false

            else -> null
        }
    }

    fun applyAnswer(input: TriageInput, step: TriageStep, answer: Boolean): TriageInput =
        when (step) {
            TriageStep.SAFETY_CHECK -> input
            TriageStep.WALK -> input.copy(canWalk = answer)
            TriageStep.BREATHING -> input.copy(isBreathing = answer)
            TriageStep.CIRCULATION -> input.copy(hasCirculation = answer)
            TriageStep.CONSCIOUSNESS -> input.copy(isConscious = answer)
            TriageStep.DONE -> input
        }

    fun evaluate(input: TriageInput): TriageResult {
        if (input.canWalk == true) return TriageResult.MINOR
        return TriageResult.SEVERE
    }

    fun toGuidance(result: TriageResult): String = when (result) {
        TriageResult.MINOR -> "【判定：軽症】傷病者は自力で最寄りの救護所へ向かうよう案内してください。"
        TriageResult.SEVERE -> "【判定：重症】傷病者を最寄りの病院へ運んでください。"
    }

    fun decideActions(result: TriageResult, input: TriageInput): TriageActionPlan {
        if (result == TriageResult.MINOR) {
            return TriageActionPlan(
                destination = "救護所（自力で移動）",
                safetyFirst = emptyList(),
                actions = listOf(
                    "傷や出血がある場合は清潔な布で押さえる",
                    "救護所へ自力で向かう"
                ),
                forbiddenActions = emptyList()
            )
        }

        val actions = mutableListOf<String>()
        val forbidden = mutableListOf<String>().also { it.addAll(globalForbiddenSevere) }

        when {
            input.isBreathing == false -> {
                actions.add("頭部を後ろに傾け気道を確保する")
                actions.add("呼吸が戻らない場合は胸骨圧迫を行う")
                actions.add("近くにAEDがあれば使用する")
                forbidden.add("首を無理に動かす")
                forbidden.add("傷病者を一人にしない")
            }
            input.hasCirculation == false -> {
                actions.add("出血部位を清潔な布で強く圧迫する")
                actions.add("圧迫を緩めず保持し続ける")
                forbidden.add("圧迫を途中で外す")
                forbidden.add("止血しないまま動かす")
            }
            input.isConscious == false -> {
                actions.add("呼吸を確認し続ける")
                actions.add("横向きに寝かせる（回復体位）")
                forbidden.add("無理に起こす")
                forbidden.add("飲食物を与える")
            }
            else -> {
                actions.add("傷や出血箇所を確認し布などで圧迫する")
                actions.add("傷病者を安静に保つ")
            }
        }

        actions.add("複数人で病院へ運ぶ")
        forbidden.add("一人だけで搬送する")

        return TriageActionPlan(
            destination = "病院（周囲の人が運ぶ）",
            safetyFirst = patientChecksSevere,
            actions = actions,
            forbiddenActions = forbidden
        )
    }
}

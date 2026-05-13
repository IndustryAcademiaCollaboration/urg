package com.urg.edge

data class TriageInput(
    val canWalk: Boolean? = null,
    val isBreathing: Boolean? = null,
    val hasCirculation: Boolean? = null,
    val isConscious: Boolean? = null
)

enum class TriageStep { WALK, BREATHING, CIRCULATION, CONSCIOUSNESS, DONE }

enum class TriageResult { MINOR, SEVERE }

object StartRuleEngine {

    val stepQuestions = mapOf(
        TriageStep.WALK to "【歩行確認】傷病者は自力で歩けますか？（はい / いいえ）",
        TriageStep.BREATHING to "【呼吸確認】呼吸はありますか？（はい / いいえ）",
        TriageStep.CIRCULATION to "【循環確認】脈はありますか？（はい / いいえ）",
        TriageStep.CONSCIOUSNESS to "【意識確認】呼びかけに反応しますか？（はい / いいえ）"
    )

    fun nextStep(current: TriageStep): TriageStep = when (current) {
        TriageStep.WALK -> TriageStep.BREATHING
        TriageStep.BREATHING -> TriageStep.CIRCULATION
        TriageStep.CIRCULATION -> TriageStep.CONSCIOUSNESS
        TriageStep.CONSCIOUSNESS -> TriageStep.DONE
        TriageStep.DONE -> TriageStep.DONE
    }

    fun parseAnswer(text: String): Boolean? {
        val t = text.trim()
        return when {
            t.contains("はい") || t.contains("yes", ignoreCase = true) || t == "y" || t == "1" -> true
            t.contains("いいえ") || t.contains("no", ignoreCase = true) || t == "n" || t == "0" -> false
            else -> null
        }
    }

    fun applyAnswer(input: TriageInput, step: TriageStep, answer: Boolean): TriageInput =
        when (step) {
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
}

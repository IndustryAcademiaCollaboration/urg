package com.urg.edge

class TriageController {

    var step: TriageStep? = null
        private set
    private var input = TriageInput()
    var lastResult: TriageResult? = null
        private set
    var lastActionPlan: TriageActionPlan? = null
        private set
    private var retryCount = 0

    val isActive: Boolean get() = step != null && step != TriageStep.DONE

    fun start(): String {
        input = TriageInput()
        step = TriageStep.SAFETY_CHECK
        retryCount = 0
        return "${Strings.TRIAGE_CALM_INTRO}STARTトリアージを開始します。\n\n${StartRuleEngine.stepQuestions[TriageStep.SAFETY_CHECK]!!}"
    }

    fun handleAnswer(text: String): TriageHandleResult {
        val currentStep = step ?: return TriageHandleResult.Ignored
        if (currentStep == TriageStep.DONE) return TriageHandleResult.Ignored

        val answer = StartRuleEngine.parseAnswer(text)
            ?: run {
                retryCount++
                val message = if (retryCount >= 2)
                    "「はい」か「いいえ」でお答えください。画面のボタンでも操作できます。\n${StartRuleEngine.stepQuestions[currentStep]!!}"
                else
                    "「はい」か「いいえ」でお答えください。\n${StartRuleEngine.stepQuestions[currentStep]!!}"
                return TriageHandleResult.InvalidAnswer(message, showButtons = retryCount >= 2)
            }

        retryCount = 0

        if (currentStep == TriageStep.SAFETY_CHECK) {
            return if (!answer) {
                step = null
                TriageHandleResult.SafetyFailed(
                    "周囲が危険です。まず自身の安全を確保し、速やかに避難してください。他の人の救助はご自身が安全な場所へ移動した後に行ってください。"
                )
            } else {
                step = TriageStep.WALK
                TriageHandleResult.NextQuestion(StartRuleEngine.stepQuestions[TriageStep.WALK]!!)
            }
        }

        input = StartRuleEngine.applyAnswer(input, currentStep, answer)

        if (currentStep == TriageStep.WALK && answer) return finalize()

        val nextStep = StartRuleEngine.nextStep(currentStep)
        step = nextStep

        return if (nextStep == TriageStep.DONE) finalize()
        else TriageHandleResult.NextQuestion(StartRuleEngine.stepQuestions[nextStep]!!)
    }

    private fun finalize(): TriageHandleResult.Done {
        step = TriageStep.DONE
        val result = StartRuleEngine.evaluate(input)
        val plan = StartRuleEngine.decideActions(result, input)
        lastResult = result
        lastActionPlan = plan
        return TriageHandleResult.Done(StartRuleEngine.toGuidance(result), result, plan, input)
    }
}

sealed class TriageHandleResult {
    object Ignored : TriageHandleResult()
    data class InvalidAnswer(val message: String, val showButtons: Boolean = false) : TriageHandleResult()
    data class SafetyFailed(val message: String) : TriageHandleResult()
    data class NextQuestion(val question: String) : TriageHandleResult()
    data class Done(
        val guidanceMessage: String,
        val result: TriageResult,
        val actionPlan: TriageActionPlan,
        val input: TriageInput
    ) : TriageHandleResult()
}

package com.urg.edge

// チャット入力に対する場所質問の検出結果
sealed class MapNavDetection {
    // 検出不発火（通常のLLM応答に流す）
    object NotApplicable : MapNavDetection()
    // 対象者を一意に特定でき、座標もある（地図遷移の確認へ進める）
    data class Found(val victim: VictimRecord) : MapNavDetection()
    // 対象者は特定できたが座標が記録されていない
    data class FoundWithoutLocation(val victim: VictimRecord) : MapNavDetection()
}

// チャット入力から「対象者の場所を尋ねる意図」をルールベースで検出する。
// ローカルLLMは応答が遅く構造化出力の信頼性も低いため、確認フローの起点は決定的なルールで判定する。
object MapNavigationDetector {

    // 場所を尋ねる意図とみなすキーワード
    private val locationKeywords = listOf("どこ", "場所", "位置", "地図", "マップ")

    // 半角の P<数字>（例: P2）。全角・音声揺れはMVP対象外。
    // 直前が英数字の場合（例: stop2, jp2）はP番号とみなさない
    private val pNumberRegex = Regex("(?<![A-Za-z0-9])[Pp](\\d+)")

    fun detect(input: String, victims: List<VictimRecord>, scope: ChatScope): MapNavDetection {
        // 場所意図がなければ不発火
        if (locationKeywords.none { input.contains(it) }) return MapNavDetection.NotApplicable

        val victim = resolveVictim(input, victims, scope) ?: return MapNavDetection.NotApplicable

        return if (victim.latitude != null && victim.longitude != null) {
            MapNavDetection.Found(victim)
        } else {
            MapNavDetection.FoundWithoutLocation(victim)
        }
    }

    // 対象者を一意に解決する。できない場合は null（＝不発火）
    private fun resolveVictim(
        input: String,
        victims: List<VictimRecord>,
        scope: ChatScope
    ): VictimRecord? {
        // 入力中の P番号 を最優先で解決する（スコープへのフォールバックはしない：
        // 存在しないP番号を指定された場合にスコープの別人へ誤マッチさせないため）
        val numbers = pNumberRegex.findAll(input)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .distinct()
            .toList()
        if (numbers.isNotEmpty()) {
            // 複数のP番号が含まれる場合は一意に絞れないため不発火
            val no = numbers.singleOrNull() ?: return null
            return victims.firstOrNull { it.displayNo == no }
        }

        // 個別スコープが1人に絞られていればその対象者
        if (scope is ChatScope.Custom && scope.displayNos.size == 1) {
            val no = scope.displayNos.first()
            return victims.firstOrNull { it.displayNo == no }
        }

        return null
    }
}

package com.urg.edge

// チャットで会話対象とする対象者の括り
sealed class ChatScope {
    abstract val label: String
    abstract fun matches(victim: VictimRecord): Boolean

    object All : ChatScope() {
        override val label = "全員"
        override fun matches(victim: VictimRecord) = true
    }
    object Severe : ChatScope() {
        override val label = "重症"
        override fun matches(victim: VictimRecord) = victim.result == TriageResult.SEVERE
    }
    object Minor : ChatScope() {
        override val label = "軽症"
        override fun matches(victim: VictimRecord) = victim.result == TriageResult.MINOR
    }

    // 1人ピックアップも複数人グループも、要素数の違うCustomとして統一的に扱う
    data class Custom(val displayNos: Set<Int>) : ChatScope() {
        override val label = "個別"
        override fun matches(victim: VictimRecord) = victim.displayNo in displayNos
    }

    companion object {
        // All はチップ表示しない（未選択＝絞り込みなしのデフォルト・フォールバックとして内部でのみ使う）
        // lazy 必須: 先に Severe 等のオブジェクト初期化が走ると、親クラスの静的初期化が
        // 割り込みで先行し、未初期化（null）のオブジェクトがリストに入ってしまう（NPEクラッシュ）
        val presets: List<ChatScope> by lazy { listOf(Severe, Minor) }
    }
}

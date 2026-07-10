package com.urg.edge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MapNavigationDetectorTest {

    private fun victim(
        displayNo: Int,
        result: TriageResult = TriageResult.SEVERE,
        latitude: Double? = 35.0,
        longitude: Double? = 139.0,
        input: TriageInput = TriageInput(),
    ): VictimRecord = VictimRecord(
        id = "id-$displayNo",
        sessionId = "session-1",
        displayNo = displayNo,
        triageInput = input,
        result = result,
        actionPlan = StartRuleEngine.decideActions(result, input),
        latitude = latitude,
        longitude = longitude,
        recordedAt = displayNo.toLong(),
    )

    // ── 場所キーワード ─────────────────────────────────────────────────

    @Test
    fun 場所キーワードとP番号があればFound() {
        val victims = listOf(victim(1), victim(2))
        val detection = MapNavigationDetector.detect("P2はどこ？", victims, ChatScope.All)
        assertIs<MapNavDetection.Found>(detection)
        assertEquals(2, detection.victim.displayNo)
    }

    @Test
    fun 各場所キーワードで発火する() {
        val victims = listOf(victim(1))
        listOf("P1はどこ", "P1の場所を教えて", "P1の位置は", "P1を地図で見たい", "P1をマップに出して").forEach { input ->
            assertIs<MapNavDetection.Found>(
                MapNavigationDetector.detect(input, victims, ChatScope.All),
                "input=$input"
            )
        }
    }

    @Test
    fun 場所キーワードがなければ不発火() {
        val victims = listOf(victim(1))
        val detection = MapNavigationDetector.detect("P1の容態を教えて", victims, ChatScope.All)
        assertIs<MapNavDetection.NotApplicable>(detection)
    }

    // ── 対象者の特定 ───────────────────────────────────────────────────

    @Test
    fun P番号もスコープ絞り込みもなければ不発火() {
        val victims = listOf(victim(1), victim(2))
        val detection = MapNavigationDetector.detect("みんなどこにいる？", victims, ChatScope.All)
        assertIs<MapNavDetection.NotApplicable>(detection)
    }

    @Test
    fun 存在しないP番号は不発火() {
        val victims = listOf(victim(1))
        val detection = MapNavigationDetector.detect("P99はどこ？", victims, ChatScope.All)
        assertIs<MapNavDetection.NotApplicable>(detection)
    }

    @Test
    fun 複数のP番号は一意に絞れず不発火() {
        val victims = listOf(victim(1), victim(2))
        val detection = MapNavigationDetector.detect("P1とP2はどこ？", victims, ChatScope.All)
        assertIs<MapNavDetection.NotApplicable>(detection)
    }

    @Test
    fun 英数字列の一部のP風文字列は不発火() {
        val victims = listOf(victim(2))
        listOf("stop2はどこ", "jp2チームはどこ").forEach { input ->
            assertIs<MapNavDetection.NotApplicable>(
                MapNavigationDetector.detect(input, victims, ChatScope.All),
                "input=$input"
            )
        }
    }

    @Test
    fun 小文字のp番号でも発火する() {
        val victims = listOf(victim(1))
        val detection = MapNavigationDetector.detect("p1はどこ？", victims, ChatScope.All)
        assertIs<MapNavDetection.Found>(detection)
    }

    @Test
    fun Customスコープ1人ならP番号なしでも発火する() {
        val victims = listOf(victim(1), victim(2))
        val detection = MapNavigationDetector.detect("この人はどこ？", victims, ChatScope.Custom(setOf(2)))
        assertIs<MapNavDetection.Found>(detection)
        assertEquals(2, detection.victim.displayNo)
    }

    @Test
    fun Customスコープ複数人は不発火() {
        val victims = listOf(victim(1), victim(2))
        val detection = MapNavigationDetector.detect("どこにいる？", victims, ChatScope.Custom(setOf(1, 2)))
        assertIs<MapNavDetection.NotApplicable>(detection)
    }

    @Test
    fun P番号はスコープより優先される() {
        val victims = listOf(victim(1), victim(2))
        val detection = MapNavigationDetector.detect("P1はどこ？", victims, ChatScope.Custom(setOf(2)))
        assertIs<MapNavDetection.Found>(detection)
        assertEquals(1, detection.victim.displayNo)
    }

    @Test
    fun 存在しないP番号はスコープにフォールバックせず不発火() {
        val victims = listOf(victim(1))
        val detection = MapNavigationDetector.detect("P99はどこ？", victims, ChatScope.Custom(setOf(1)))
        assertIs<MapNavDetection.NotApplicable>(detection)
    }

    // ── 座標なし ───────────────────────────────────────────────────────

    @Test
    fun 座標がnullならFoundWithoutLocation() {
        val victims = listOf(victim(1, latitude = null, longitude = null))
        val detection = MapNavigationDetector.detect("P1はどこ？", victims, ChatScope.All)
        assertIs<MapNavDetection.FoundWithoutLocation>(detection)
        assertEquals(1, detection.victim.displayNo)
    }

    @Test
    fun 座標が片方nullでもFoundWithoutLocation() {
        val victims = listOf(victim(1, latitude = 35.0, longitude = null))
        val detection = MapNavigationDetector.detect("P1はどこ？", victims, ChatScope.All)
        assertIs<MapNavDetection.FoundWithoutLocation>(detection)
    }

    @Test
    fun Customスコープ1人でも座標なしはFoundWithoutLocation() {
        val victims = listOf(victim(1, latitude = null, longitude = null))
        val detection = MapNavigationDetector.detect("場所を教えて", victims, ChatScope.Custom(setOf(1)))
        assertIs<MapNavDetection.FoundWithoutLocation>(detection)
    }
}

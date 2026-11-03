package com.team3.dorawing.ui.guardian

import com.team3.dorawing.db.Attendance
import com.team3.dorawing.db.ChangeAlert
import com.team3.dorawing.db.DiagnosisResult
import com.team3.dorawing.game.GameType
import com.team3.dorawing.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class SummaryTest {

    private val today = LocalDate.of(2026, 11, 3)
    private val now = Dates.toMillis(today) + 16 * 3_600_000L

    private fun sample(): Summary {
        val types = GameType.entries.map { it.name }
        return Summary.build(
            today = today,
            now = now,
            diagnosis = DiagnosisResult(id = 3, takenAt = now - 3_600_000L, totalScore = 7, isSuspected = true),
            rates = types.associateWith { if (it == types.first()) null else 0.7f },
            levels = types.associateWith { 3 },
            attendance = listOf(
                Attendance("2026-10-01", 2),            // 14일보다 오래됨
                Attendance("2026-11-02", 1),
                Attendance("2026-11-03", 0),            // 세트 없음
                Attendance("2026-11-01", 3)
            ),
            lastPlayedAt = now - 60_000L,
            alerts = listOf(
                ChangeAlert(1, types.first(), 0.4f, 0.8f, Dates.toMillis(today.minusDays(20))),
                ChangeAlert(2, types.last(), 0.5f, 0.75f, Dates.toMillis(today.minusDays(1)), dismissed = true)
            )
        )
    }

    // 맵 안의 모든 키를 모은다 (영역 이름 키는 빼고)
    private fun keysOf(value: Any?, out: MutableSet<String> = mutableSetOf()): Set<String> {
        when (value) {
            is Map<*, *> -> value.forEach { (k, v) ->
                val key = k.toString()
                if (GameType.fromName(key) == null) out += key
                keysOf(v, out)
            }
            is List<*> -> value.forEach { keysOf(it, out) }
        }
        return out
    }

    @Test
    fun `올리는 칸은 정해 둔 것뿐이다`() {
        val map = sample().toMap()
        assertEquals(Summary.KEYS, map.keys)
        val allowed = Summary.KEYS + setOf("score", "maxScore", "suspected", "takenAt", "rate", "level", "area")
        assertEquals(emptySet<String>(), keysOf(map) - allowed)
    }

    @Test
    fun `공유하지 않는 항목은 올라가지 않는다`() {
        val keys = keysOf(sample().toMap()).map { it.lowercase() }
        // 문항별 답, 이름, 연락처, 위치, 게임 세트별 기록
        val forbidden = listOf(
            "answer", "answers", "name", "phone", "tel", "contact", "location", "lat", "lng", "address",
            "correct", "total", "avgreactionms", "playedat", "sets", "id", "recentavg", "baselineavg"
        )
        forbidden.forEach { assertFalse("$it 칸이 있으면 안 된다", it in keys) }
        // 자가진단은 총점과 결과만 (기록 번호도 올리지 않는다)
        val diag = sample().toMap()["diagnosis"] as Map<*, *>
        assertEquals(setOf("score", "maxScore", "suspected", "takenAt"), diag.keys)
    }

    @Test
    fun `출석은 지난 14일 중 세트를 한 날만`() {
        assertEquals(listOf("2026-11-01", "2026-11-02"), sample().attendDays)
    }

    @Test
    fun `변화 알림은 최근 7일 것만`() {
        val alerts = sample().alerts
        assertEquals(1, alerts.size)
        assertEquals(GameType.entries.last().name, alerts[0].area)
        assertEquals("2026-11-02", alerts[0].day)
    }

    @Test
    fun `영역은 GameType 목록대로 정답률과 단계`() {
        val areas = sample().areas
        assertEquals(GameType.entries.map { it.name }.toSet(), areas.keys)
        assertNull(areas[GameType.entries.first().name]!!.rate)
        assertEquals(0.7, areas[GameType.entries.last().name]!!.rate!!, 0.0001)
        assertTrue(areas.values.all { it.level == 3 })
    }

    @Test
    fun `서버에서 받은 값으로 다시 만들 수 있다`() {
        val summary = sample()
        // Firestore는 Int를 Long으로 돌려준다
        fun toServer(value: Any?): Any? = when (value) {
            is Int -> value.toLong()
            is Map<*, *> -> value.mapValues { toServer(it.value) }
            is List<*> -> value.map { toServer(it) }
            else -> value
        }
        @Suppress("UNCHECKED_CAST")
        val back = Summary.fromMap(toServer(summary.toMap()) as Map<String, Any?>)
        assertEquals(summary, back)
    }

    @Test
    fun `서버 규칙의 요약 칸과 앱의 칸이 같다`() {
        val rules = File("../firestore.rules").readText()
        val list = Regex("function validSummary\\(\\)[\\s\\S]*?hasOnly\\(\\[([^\\]]*)]").find(rules)!!.groupValues[1]
        val keys = Regex("'([^']+)'").findAll(list).map { it.groupValues[1] }.toSet()
        assertEquals(Summary.KEYS, keys)
    }
}

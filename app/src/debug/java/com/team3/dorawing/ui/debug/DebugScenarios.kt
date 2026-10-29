package com.team3.dorawing.ui.debug

import com.team3.dorawing.game.GameType
import kotlin.random.Random

// 디버그 메뉴가 넣을 가상 세트. order는 같은 날 안에서 넣는 순서
data class PlannedSet(val type: GameType, val correct: Int, val daysAgo: Int, val order: Int)

// 화면 없이 테스트할 수 있게 데이터 만드는 부분만 따로 뒀다
object DebugScenarios {
    // 20문항으로 넣어 정답률을 5% 단위로 맞춘다
    const val TOTAL = 20

    // 누를 때마다 같은 결과가 나오게 씨앗값을 고정한다
    private const val SEED = 2026

    // 1. 모든 영역 지난 35일 동안 매일 1세트, 정답률 60~80%
    fun steady(): List<PlannedSet> {
        val random = Random(SEED)
        return days { _, _ -> normal(random) }
    }

    // 2. 1번과 같지만 첫 영역만 최근 7일 정답률 30~45%
    fun oneDrop(): List<PlannedSet> {
        val random = Random(SEED)
        val dropType = GameType.entries.first()
        return days { type, daysAgo ->
            if (type == dropType && daysAgo < 7) random.nextInt(6, 10) else normal(random)
        }
    }

    // 3. 오늘 앞의 두 영역 1세트씩 정답률 90%
    fun todayTwo(): List<PlannedSet> =
        GameType.entries.take(2).mapIndexed { i, type -> PlannedSet(type, 18, 0, i) }

    // 12~16개 = 60~80%
    private fun normal(random: Random) = random.nextInt(12, 17)

    private fun days(correctOf: (GameType, Int) -> Int): List<PlannedSet> {
        val sets = mutableListOf<PlannedSet>()
        for (daysAgo in 34 downTo 0) {
            for ((i, type) in GameType.entries.withIndex()) {
                sets += PlannedSet(type, correctOf(type, daysAgo), daysAgo, i)
            }
        }
        return sets
    }
}

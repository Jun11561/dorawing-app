package com.team3.dorawing.ui.game

// 기억력 게임의 레벨별 규칙. 화면과 떼어 두어 단위 테스트로 확인한다
object MemoryRules {
    const val PAIRS = "카드 짝맞추기"
    const val ORDER = "순서대로 누르기"
    const val RECALL = "있었던 것 고르기"

    // 레벨별 세트 구성. 레벨이 오를수록 순서대로 누르기와 있었던 것 고르기가 더 자주 나온다
    fun problems(level: Int): List<String> = when (level) {
        1 -> listOf(PAIRS, PAIRS, ORDER, RECALL)
        2 -> listOf(PAIRS, ORDER, RECALL, PAIRS, ORDER)
        3 -> listOf(PAIRS, ORDER, RECALL, ORDER, RECALL)
        4 -> listOf(PAIRS, ORDER, RECALL, ORDER, RECALL, ORDER)
        else -> listOf(PAIRS, ORDER, RECALL, ORDER, RECALL, RECALL, ORDER)
    }

    // 카드 짝맞추기 판 (줄 수, 칸 수). 홀수 장수는 짝이 남아서 쓰지 않고, 누르기 편하게 가로는 4칸까지
    fun pairBoard(level: Int): Pair<Int, Int> = when (level) {
        1 -> 2 to 2
        2 -> 2 to 3
        3 -> 4 to 3
        4 -> 4 to 4
        else -> 5 to 4
    }

    // 짝맞추기에서 틀려도 맞힌 것으로 보는 횟수: 짝 수만큼
    fun pairMistakesAllowed(level: Int): Int {
        val (rows, cols) = pairBoard(level)
        return rows * cols / 2
    }

    // 순서대로 누르기 판과 불이 켜지는 횟수
    fun orderBoard(level: Int): Pair<Int, Int> = when (level) {
        1 -> 2 to 2
        2, 3 -> 3 to 3
        else -> 4 to 4
    }

    fun orderLength(level: Int): Int = when (level) {
        1 -> 2
        2, 3 -> 3
        4 -> 4
        else -> 5
    }

    // 있었던 것 고르기: 보여 주는 그림 수, 보기 수, 보여 주는 시간
    fun recallShown(level: Int): Int = (level + 2).coerceIn(3, 7)

    fun recallChoices(level: Int): Int = if (level >= 4) 6 else 4

    // 그림 하나에 1.2초, 적어도 3초
    fun recallShowMs(level: Int): Long = maxOf(3000L, recallShown(level) * 1200L)
}

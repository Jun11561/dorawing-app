package com.team3.dorawing.ui.game

import kotlin.random.Random

// 주의력 게임(색깔 글자 맞추기)의 규칙. 스트룹 검사 방식
object AttentionRules {
    const val SAME = "같은 색"
    const val DIFF = "다른 색"
    const val SWITCH = "규칙 바꾸기"

    // 색 이름과 값. 레벨이 오르면 앞에서부터 더 많이 쓴다
    val COLORS = listOf(
        "빨강" to 0xFFD23B2F.toInt(),
        "파랑" to 0xFF2B5BD7.toInt(),
        "초록" to 0xFF2E8B57.toInt(),
        "노랑" to 0xFFD9A400.toInt(),
        "검정" to 0xFF2E2A24.toInt()
    )

    // 문제 하나. word는 글자의 뜻, ink는 글자의 색, framed는 테두리가 있는지 (COLORS 번호)
    data class Problem(val word: Int, val ink: Int, val framed: Boolean, val answer: Int)

    // 레벨별 세트 구성. 레벨이 오를수록 다른 색과 규칙 바꾸기가 더 자주 나온다
    fun problems(level: Int): List<String> = when (level) {
        1 -> listOf(SAME, SAME, DIFF, SAME, DIFF, SWITCH)
        2 -> listOf(SAME, DIFF, SAME, DIFF, SWITCH, DIFF, SWITCH)
        3 -> listOf(SAME, DIFF, SWITCH, DIFF, SWITCH, DIFF, SWITCH, SAME)
        4 -> listOf(DIFF, SWITCH, DIFF, SWITCH, SAME, DIFF, SWITCH, DIFF, SWITCH)
        else -> listOf(DIFF, SWITCH, SWITCH, DIFF, SWITCH, DIFF, SWITCH, SWITCH, DIFF, SWITCH)
    }

    // 보기로 나오는 색 수
    fun colorCount(level: Int): Int = when (level) {
        1, 2 -> 3
        3, 4 -> 4
        else -> 5
    }

    fun make(kind: String, level: Int, random: Random = Random.Default): Problem {
        val n = colorCount(level)
        val word = random.nextInt(n)
        // 뜻과 다른 색
        val other = (word + 1 + random.nextInt(n - 1)) % n
        return when (kind) {
            SAME -> Problem(word, word, false, word)
            DIFF -> Problem(word, other, false, other)
            else -> {
                // 테두리가 있으면 글자의 뜻, 없으면 글자의 색
                val framed = random.nextBoolean()
                Problem(word, other, framed, if (framed) word else other)
            }
        }
    }
}

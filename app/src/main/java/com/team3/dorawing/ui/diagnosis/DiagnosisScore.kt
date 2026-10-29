package com.team3.dorawing.ui.diagnosis

// KDSQ-C 방식 점수 계산. 문항마다 0(아니다), 1(가끔 그렇다), 2(자주 그렇다)
object DiagnosisScore {
    // 국가건강검진 기준: 0~5점 정상, 6점 이상 인지 저하 의심
    const val SUSPECT_SCORE = 6
    const val MAX_SCORE = 30

    fun total(answers: List<Int>): Int {
        require(answers.all { it in 0..2 }) { "답은 0, 1, 2 중 하나여야 해요" }
        return answers.sum()
    }

    fun isSuspected(total: Int): Boolean = total >= SUSPECT_SCORE
}

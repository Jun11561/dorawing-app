package com.team3.dorawing.ui.game

import kotlin.random.Random

// 계산력 게임(시장 보기)의 규칙. 가격은 실제 시세가 아니라 문제 속 가게 가격표
object CalcRules {
    const val SUM = "얼마예요?"
    const val PAY = "돈 내기"
    const val TIME = "몇 시에 도착?"

    data class Item(val pic: String, val name: String, val price: Int)

    // 천 원 단위 물건 (1단계)
    val THOUSANDS = listOf(
        Item("🍞", "빵", 1000), Item("🥛", "우유", 2000), Item("🥚", "달걀", 3000),
        Item("🐟", "생선", 4000), Item("🍉", "수박", 5000), Item("🍗", "닭고기", 6000)
    )

    // 백 원 단위 물건 (2단계부터)
    val HUNDREDS = listOf(
        Item("🥕", "당근", 700), Item("🌱", "콩나물", 800), Item("🧅", "양파", 900),
        Item("🍎", "사과", 1200), Item("🍊", "귤", 1500), Item("🌽", "옥수수", 1800),
        Item("🍌", "바나나", 2200), Item("🥬", "배추", 2500), Item("🍠", "고구마", 3500)
    )

    // 지폐와 동전
    val MONEY = listOf(10000, 5000, 1000, 500, 100)

    // 얼마예요 문제: 물건과 개수, 낸 돈(거스름돈 문제일 때만), 정답, 보기 4개
    data class SumProblem(val items: List<Pair<Item, Int>>, val pay: Int?, val answer: Int, val choices: List<Int>)

    // 몇 시에 도착 문제: 출발 시각(분), 걸리는 시간(분), 정답(분), 보기(분)
    data class TimeProblem(val start: Int, val takes: Int, val answer: Int, val choices: List<Int>)

    fun problems(level: Int): List<String> = when (level) {
        1 -> listOf(SUM, SUM, PAY, SUM, TIME)
        2 -> listOf(SUM, PAY, SUM, TIME, PAY)
        3 -> listOf(SUM, PAY, TIME, SUM, PAY, TIME)
        4 -> listOf(SUM, PAY, TIME, SUM, PAY, TIME)
        else -> listOf(SUM, PAY, TIME, SUM, PAY, TIME, SUM)
    }

    // 레벨별 연산: 1 덧셈 천 원, 2 덧셈 백 원 3개, 3 거스름돈, 4 곱셈, 5 섞기 만 원 단위
    fun makeSum(level: Int, random: Random = Random.Default): SumProblem {
        val items: List<Pair<Item, Int>>
        var pay: Int? = null
        when (level) {
            1 -> items = THOUSANDS.shuffled(random).take(2).map { it to 1 }
            2 -> items = HUNDREDS.shuffled(random).take(3).map { it to 1 }
            3 -> {
                items = HUNDREDS.shuffled(random).take(2).map { it to 1 }
                pay = payFor(items.sumOf { it.first.price * it.second })
            }
            4 -> items = listOf(HUNDREDS.shuffled(random).first() to (2 + random.nextInt(3)))
            else -> {
                val two = HUNDREDS.shuffled(random).take(2)
                items = listOf(two[0] to (2 + random.nextInt(2)), two[1] to 1)
                pay = 20000
            }
        }
        val total = items.sumOf { it.first.price * it.second }
        val answer = if (pay == null) total else pay - total
        return SumProblem(items, pay, answer, choicesAround(answer, random))
    }

    // 거스름돈 문제에서 내는 돈: 합계보다 큰 가장 가까운 천 원 단위에 천 원을 더한 값 (예: 3,700원 → 5,000원)
    fun payFor(total: Int): Int = (total / 1000 + 1) * 1000 + 1000

    // 정답 근처의 다른 값 3개를 섞어 보기 4개
    fun choicesAround(answer: Int, random: Random = Random.Default): List<Int> {
        val candidates = listOf(1000, -1000, 500, -500, 2000, -2000, 1500, -1500)
            .map { answer + it }
            .filter { it > 0 && it != answer }
            .distinct()
        return (listOf(answer) + candidates.shuffled(random).take(3)).shuffled(random)
    }

    // 돈 내기: 레벨별 금액. 1단계는 천 원 단위, 2단계 오백 원, 그 뒤는 백 원 단위로 점점 커진다
    fun makePay(level: Int, random: Random = Random.Default): Int = when (level) {
        1 -> (1 + random.nextInt(5)) * 1000
        2 -> (1 + random.nextInt(10)) * 500
        3 -> (5 + random.nextInt(46)) * 100
        4 -> (10 + random.nextInt(91)) * 100
        else -> (50 + random.nextInt(151)) * 100
    }

    // 낮은 단계에서는 동전을 쓰지 않아 지폐만 보여 준다
    fun moneyFor(level: Int): List<Int> = if (level == 1) MONEY.filter { it >= 1000 } else MONEY

    // 몇 시에 도착: 시각은 분으로 센다 (2시 = 120). 12시를 넘지 않게 출발은 9시까지
    fun makeTime(level: Int, random: Random = Random.Default): TimeProblem {
        val takes = when (level) {
            1, 2 -> listOf(30, 60).random(random)
            3 -> listOf(30, 60, 90).random(random)
            4 -> listOf(60, 90, 120).random(random)
            else -> listOf(90, 120, 150).random(random)
        }
        val hour = 1 + random.nextInt(9)
        val half = level >= 3 && random.nextBoolean()
        val start = hour * 60 + if (half) 30 else 0
        val answer = start + takes
        val candidates = listOf(-60, -30, 30, 60, 90)
            .map { answer + it }
            .filter { it in 60..(12 * 60) && it != answer }
        val choices = (listOf(answer) + candidates.shuffled(random).take(3)).shuffled(random)
        return TimeProblem(start, takes, answer, choices)
    }

    // 분 → "3시 30분"
    fun timeText(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "${h}시" else "${h}시 ${m}분"
    }

    // 분 → 시계 그림 (이모지는 30분 단위까지 있다)
    fun clockEmoji(minutes: Int): String {
        val h = ((minutes / 60 - 1) % 12 + 12) % 12
        val half = minutes % 60 >= 30
        val code = (if (half) 0x1F55C else 0x1F550) + h
        return String(Character.toChars(code))
    }

    // 걸리는 시간 → "1시간 30분"
    fun takesText(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "${m}분"
            m == 0 -> "${h}시간"
            else -> "${h}시간 ${m}분"
        }
    }
}

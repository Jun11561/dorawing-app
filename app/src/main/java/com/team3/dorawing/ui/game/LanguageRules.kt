package com.team3.dorawing.ui.game

import kotlin.random.Random

// 언어력 게임(낱말과 속담)의 규칙과 문제 데이터
object LanguageRules {
    const val BLANK = "빈칸 채우기"
    const val NAME = "이름 고르기"
    const val GROUP = "같은 종류 고르기"

    // 속담: 빈칸 앞, 정답, 빈칸 뒤, 틀린 보기 4개. 앞 10개가 더 익숙한 것.
    // 빈칸 뒤가 조사("까지 간다")면 붙이고 낱말("말이 곱다")이면 앞에 공백을 둔다
    data class Proverb(val before: String, val answer: String, val after: String, val wrong: List<String>)

    val PROVERBS = listOf(
        Proverb("가는 말이 고와야", "오는", " 말이 곱다", listOf("하는", "듣는", "보는", "쓰는")),
        Proverb("등잔 밑이", "어둡다", "", listOf("밝다", "뜨겁다", "차갑다", "넓다")),
        Proverb("티끌 모아", "태산", "", listOf("바다", "동산", "하늘", "강물")),
        Proverb("세 살 버릇", "여든", "까지 간다", listOf("열", "스물", "백", "일흔")),
        Proverb("원숭이도", "나무", "에서 떨어진다", listOf("지붕", "바위", "하늘", "산")),
        Proverb("소 잃고", "외양간", " 고친다", listOf("대문", "마당", "지붕", "울타리")),
        Proverb("발 없는", "말", "이 천 리 간다", listOf("소문", "바람", "노래", "글")),
        Proverb("누워서", "떡", " 먹기", listOf("밥", "죽", "과일", "빵")),
        Proverb("하늘의", "별", " 따기", listOf("달", "해", "구름", "바람")),
        Proverb("우물 안", "개구리", "", listOf("두꺼비", "물고기", "거북이", "올챙이")),
        Proverb("가재는", "게", " 편", listOf("새우", "조개", "개구리", "물고기")),
        Proverb("백지장도", "맞들면", " 낫다", listOf("접으면", "펴면", "버리면", "들면")),
        Proverb("낮말은 새가 듣고 밤말은", "쥐", "가 듣는다", listOf("개", "고양이", "소", "닭")),
        Proverb("돌다리도", "두들겨", " 보고 건너라", listOf("뛰어", "밟아", "바라", "만져")),
        Proverb("고래 싸움에", "새우", " 등 터진다", listOf("멸치", "조개", "게", "오징어")),
        Proverb("바늘 가는 데", "실", " 간다", listOf("옷", "천", "단추", "가위")),
        Proverb("윗물이 맑아야", "아랫물", "이 맑다", listOf("바닷물", "샘물", "빗물", "강물")),
        Proverb("꿩 대신", "닭", "", listOf("오리", "참새", "거위", "비둘기"))
    )

    // 이름 고르기: 그림과 이름. 1~3단계는 익숙한 물건, 4단계부터 덜 익숙한 물건
    val EASY_THINGS = listOf(
        "🍎" to "사과", "🏠" to "집", "🚗" to "자동차", "☂️" to "우산", "⌚" to "시계", "📖" to "책",
        "🌷" to "꽃", "🐟" to "물고기", "🔑" to "열쇠", "✂️" to "가위", "🧢" to "모자", "👓" to "안경",
        "🥚" to "달걀", "🍞" to "빵", "🐶" to "강아지", "🌙" to "달", "🚲" to "자전거", "🎈" to "풍선"
    )
    val HARD_THINGS = listOf(
        "☎️" to "전화기", "🔨" to "망치", "🕯️" to "양초", "🔔" to "종", "🎻" to "바이올린", "🥢" to "젓가락",
        "🥄" to "숟가락", "🧤" to "장갑", "🧣" to "목도리", "🧲" to "자석", "🧹" to "빗자루", "🧺" to "바구니",
        "🔦" to "손전등", "📻" to "라디오", "📷" to "사진기", "⚖️" to "저울", "🧮" to "주판", "🥁" to "북",
        "⏰" to "자명종", "🧶" to "털실"
    )

    // 같은 종류 고르기: 종류 이름과 낱말들
    val GROUPS = mapOf(
        "과일" to listOf("사과", "배", "포도", "귤", "수박", "딸기", "참외"),
        "채소" to listOf("배추", "당근", "오이", "양파", "호박", "무", "시금치"),
        "동물" to listOf("강아지", "고양이", "토끼", "소", "돼지", "말", "염소"),
        "새" to listOf("참새", "비둘기", "까치", "제비", "까마귀", "오리"),
        "가구" to listOf("의자", "책상", "침대", "장롱", "소파", "서랍장"),
        "그릇" to listOf("숟가락", "젓가락", "냄비", "접시", "컵", "주전자"),
        "탈것" to listOf("자동차", "버스", "기차", "자전거", "트럭", "비행기"),
        "옷" to listOf("바지", "치마", "셔츠", "외투", "조끼", "잠바"),
        "꽃" to listOf("장미", "튤립", "국화", "해바라기", "백합", "개나리")
    )

    // 4단계부터는 헷갈리는 종류에서 틀린 보기를 가져온다
    val CONFUSING = mapOf(
        "과일" to "채소", "채소" to "과일", "동물" to "새", "새" to "동물",
        "가구" to "그릇", "그릇" to "가구", "꽃" to "채소", "탈것" to "가구", "옷" to "가구"
    )

    // 문제 하나: 화면에 보여 줄 글(속담 또는 그림), 보기, 정답 번호, 정답 낱말, 틀렸을 때 읽어 줄 정답 문장
    data class Problem(val prompt: String, val choices: List<String>, val answer: Int, val group: String = "")

    fun problems(level: Int): List<String> = when (level) {
        1 -> listOf(BLANK, NAME, GROUP, BLANK, NAME)
        2 -> listOf(BLANK, NAME, GROUP, GROUP, BLANK)
        3 -> listOf(BLANK, NAME, GROUP, BLANK, NAME, GROUP)
        4 -> listOf(BLANK, NAME, GROUP, BLANK, NAME, GROUP)
        else -> listOf(BLANK, NAME, GROUP, BLANK, NAME, GROUP, BLANK)
    }

    // 보기 수. 레벨이 오르면 늘어난다
    fun choiceCount(level: Int): Int = when (level) {
        1, 2 -> 3
        3, 4 -> 4
        else -> 5
    }

    fun makeBlank(level: Int, random: Random = Random.Default): Problem {
        val pool = if (level <= 2) PROVERBS.take(10) else PROVERBS
        val p = pool.random(random)
        val choices = (listOf(p.answer) + p.wrong.shuffled(random).take(choiceCount(level) - 1)).shuffled(random)
        val prompt = p.before + " [    ]" + p.after
        return Problem(prompt, choices, choices.indexOf(p.answer))
    }

    // 받침에 따라 조사를 붙인다. josa("사과", "은", "는") → "사과는", josa("책", "이에요", "예요") → "책이에요"
    fun josa(word: String, withBatchim: String, without: String): String {
        val last = word.lastOrNull { it in '가'..'힣' || it in '0'..'9' } ?: return word + withBatchim
        // 숫자는 읽는 소리로 본다: 0 영, 1 일, 3 삼, 6 육, 7 칠, 8 팔은 받침 있음
        val hasBatchim = if (last in '0'..'9') last in "013678" else (last - '가') % 28 != 0
        return word + if (hasBatchim) withBatchim else without
    }

    // 속담 전체 문장 (정답을 넣어서). 틀렸을 때 읽어 준다
    fun fullProverb(prompt: String, answer: String): String = prompt.replace("[    ]", answer)

    fun makeName(level: Int, random: Random = Random.Default): Problem {
        val pool = if (level <= 3) EASY_THINGS else HARD_THINGS
        val pick = pool.random(random)
        val wrong = pool.filter { it != pick }.shuffled(random).take(choiceCount(level) - 1).map { it.second }
        val choices = (listOf(pick.second) + wrong).shuffled(random)
        return Problem(pick.first, choices, choices.indexOf(pick.second))
    }

    fun makeGroup(level: Int, random: Random = Random.Default): Problem {
        val group = GROUPS.keys.random(random)
        val members = GROUPS.getValue(group).shuffled(random).take(choiceCount(level) - 1)
        // 틀린 보기(정답): 4단계부터는 헷갈리는 종류에서, 그 전에는 아무 다른 종류에서
        val otherGroup = if (level >= 4) CONFUSING.getValue(group) else GROUPS.keys.filter { it != group }.random(random)
        val odd = GROUPS.getValue(otherGroup).random(random)
        val choices = (members + odd).shuffled(random)
        return Problem(group, choices, choices.indexOf(odd), group)
    }
}

package com.team3.dorawing.ui.game

import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGameMemoryBinding

// 기억력 게임. 카드 짝맞추기, 순서대로 누르기, 있었던 것 고르기 세 유형을 한 세트 안에서 섞어 낸다.
// 레벨별 판 크기와 세트 구성은 MemoryRules에 있다
class MemoryGameActivity : BaseGameActivity() {
    private lateinit var binding: ActivityGameMemoryBinding
    private val cards = mutableListOf<TextView>()
    private var kind = ""
    private var busy = false // 보여 주는 중이라 누를 수 없음

    // 카드 짝맞추기
    private var faces = listOf<Int>() // 카드마다 그림 번호
    private var open = -1             // 지금 뒤집혀 있는 카드 한 장
    private var found = 0
    private var mistakes = 0
    private val done = mutableSetOf<Int>()

    // 순서대로 누르기
    private var order = listOf<Int>()
    private var step = 0

    // 있었던 것 고르기
    private var shown = listOf<Int>()
    private var choices = listOf<Int>()
    private var answerPic = -1

    override fun contentLayout() = R.layout.activity_game_memory

    override fun bindContent(content: View) {
        binding = ActivityGameMemoryBinding.bind(content)
    }

    override fun makeProblems(level: Int) = MemoryRules.problems(level)

    override fun showProblem(kind: String, retry: Boolean) {
        this.kind = kind
        busy = false
        when (kind) {
            MemoryRules.PAIRS -> startPairs()
            MemoryRules.ORDER -> startOrder()
            else -> startRecall()
        }
    }

    override fun onHint() {
        if (busy) return
        when (kind) {
            // 짝맞추기: 아직 못 찾은 카드를 잠깐 모두 보여 준다
            MemoryRules.PAIRS -> {
                busy = true
                say(getString(R.string.game_mem_hint_pairs))
                cards.forEachIndexed { i, card -> if (i !in done) paintFace(card, faces[i]) }
                handler.postDelayed({
                    cards.forEachIndexed { i, card -> if (i !in done && i != open) paintBack(card) }
                    busy = false
                }, 1500)
            }
            // 순서: 불 켜지는 순서를 처음부터 다시 보여 준다
            MemoryRules.ORDER -> {
                say(getString(R.string.game_mem_hint_order))
                playOrder()
            }
            // 고르기: 아까 본 그림을 잠깐 다시 보여 준다
            else -> {
                say(getString(R.string.game_mem_hint_recall))
                showRecallPics()
                handler.postDelayed({ showRecallChoices(false) }, 1500)
            }
        }
    }

    // ---------- 카드 짝맞추기 ----------

    private fun startPairs() {
        val (rows, cols) = MemoryRules.pairBoard(level)
        val picks = PICS.indices.shuffled().take(rows * cols / 2)
        faces = (picks + picks).shuffled()
        open = -1
        found = 0
        mistakes = 0
        done.clear()
        buildGrid(rows, cols) { tapPair(it) }
        cards.forEach { paintBack(it) }
        setGuide(getString(R.string.game_mem_pairs_guide), getString(R.string.game_mem_pairs_speech))
    }

    private fun tapPair(i: Int) {
        if (i in done || i == open) return
        paintFace(cards[i], faces[i])
        if (open < 0) {
            open = i
            return
        }
        val first = open
        open = -1
        if (faces[first] == faces[i]) {
            done.add(first)
            done.add(i)
            paintFace(cards[first], faces[first], true)
            paintFace(cards[i], faces[i], true)
            found++
            if (found == faces.size / 2) {
                // 판 전체가 한 문제. 틀린 횟수가 짝 수 이하면 맞힌 것으로 본다. 다시 하기는 없다
                val ok = mistakes <= MemoryRules.pairMistakesAllowed(level)
                busy = true
                answered(ok, getString(R.string.game_mem_pairs_wrong), canRetry = false)
            } else {
                ding()
            }
        } else {
            mistakes++
            busy = true
            handler.postDelayed({
                paintBack(cards[first])
                paintBack(cards[i])
                busy = false
            }, 800)
        }
    }

    // ---------- 순서대로 누르기 ----------

    private fun startOrder() {
        val (rows, cols) = MemoryRules.orderBoard(level)
        val n = MemoryRules.orderLength(level)
        // 같은 칸이 두 번 나오지 않게
        order = (0 until rows * cols).shuffled().take(n)
        buildGrid(rows, cols) { tapOrder(it) }
        cards.forEach { paintCell(it, false) }
        setGuide(getString(R.string.game_mem_order_guide), getString(R.string.game_mem_order_speech, n))
        playOrder()
    }

    // 순서대로 불을 켜서 보여 준다. 보는 동안은 누를 수 없다
    private fun playOrder() {
        busy = true
        step = 0
        cards.forEach { paintCell(it, false) }
        order.forEachIndexed { k, cell ->
            val at = 800L + k * 900L
            handler.postDelayed({ paintCell(cards[cell], true) }, at)
            handler.postDelayed({ paintCell(cards[cell], false) }, at + 600L)
        }
        handler.postDelayed({
            busy = false
            say(getString(R.string.game_mem_order_now))
        }, 800L + order.size * 900L)
    }

    private fun tapOrder(i: Int) {
        if (i == order[step]) {
            paintCell(cards[i], true)
            step++
            if (step == order.size) {
                busy = true
                answered(true)
            } else {
                ding()
                handler.postDelayed({ paintCell(cards[i], false) }, 300)
            }
        } else {
            // 틀리면 맞는 순서를 칸에 숫자로 보여 준다
            busy = true
            cards.forEach { paintCell(it, false) }
            order.forEachIndexed { k, cell ->
                paintCell(cards[cell], true)
                cards[cell].text = (k + 1).toString()
            }
            answered(false, getString(R.string.game_mem_order_wrong))
        }
    }

    // ---------- 있었던 것 고르기 ----------

    private fun startRecall() {
        val n = MemoryRules.recallShown(level)
        val m = MemoryRules.recallChoices(level)
        val picks = PICS.indices.shuffled()
        shown = picks.take(n)
        answerPic = shown.random()
        // 보기: 있었던 것 하나와 없었던 것들
        choices = (listOf(answerPic) + picks.drop(n).take(m - 1)).shuffled()
        showRecallPics()
        setGuide(getString(R.string.game_mem_recall_guide), getString(R.string.game_mem_recall_speech))
        handler.postDelayed({ showRecallChoices(true) }, MemoryRules.recallShowMs(level))
    }

    private fun showRecallPics() {
        busy = true
        val cols = if (shown.size <= 4) 2 else 3
        val rows = (shown.size + cols - 1) / cols
        buildGrid(rows, cols) {}
        cards.forEachIndexed { i, card ->
            if (i < shown.size) paintFace(card, shown[i]) else card.visibility = View.INVISIBLE
        }
    }

    private fun showRecallChoices(speak: Boolean) {
        buildGrid(choices.size / 2, 2) { tapRecall(it) }
        cards.forEachIndexed { i, card -> paintFace(card, choices[i]) }
        busy = false
        if (speak) setGuide(getString(R.string.game_mem_recall_pick))
        else base.tvGuide.text = getString(R.string.game_mem_recall_pick)
    }

    private fun tapRecall(i: Int) {
        busy = true
        if (choices[i] == answerPic) {
            paintFace(cards[i], answerPic, true)
            answered(true)
        } else {
            // 정답 카드를 초록으로 보여 준다
            cards.forEachIndexed { k, card -> if (choices[k] == answerPic) paintFace(card, answerPic, true) }
            answered(false, getString(R.string.game_mem_recall_wrong, PICS[answerPic].second))
        }
    }

    // ---------- 카드 그리기 ----------

    // 줄 수와 칸 수대로 카드를 깔고, 누르면 onTap(번호). 카드는 남는 자리를 채워 크게 보인다
    private fun buildGrid(rows: Int, cols: Int, onTap: (Int) -> Unit) {
        binding.board.removeAllViews()
        cards.clear()
        val gap = resources.getDimensionPixelSize(R.dimen.game_card_gap) / 2
        val textSize = resources.getDimension(R.dimen.game_card_text)
        for (r in 0 until rows) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            for (c in 0 until cols) {
                val i = r * cols + c
                val card = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                        .apply { setMargins(gap, gap, gap, gap) }
                    gravity = Gravity.CENTER
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                    setOnClickListener { if (!busy) onTap(i) }
                }
                cards.add(card)
                row.addView(card)
            }
            binding.board.addView(row)
        }
    }

    private fun paintBack(card: TextView) {
        card.text = ""
        card.setBackgroundResource(R.drawable.game_card_back)
    }

    private fun paintFace(card: TextView, pic: Int, done: Boolean = false) {
        card.text = PICS[pic].first
        card.setTextColor(ContextCompat.getColor(this, R.color.text_main))
        card.setBackgroundResource(if (done) R.drawable.game_card_done else R.drawable.game_card_front)
    }

    private fun paintCell(cell: TextView, on: Boolean) {
        cell.text = ""
        cell.setTextColor(ContextCompat.getColor(this, if (on) R.color.white else R.color.text_main))
        cell.setBackgroundResource(if (on) R.drawable.game_cell_on else R.drawable.game_cell_off)
    }

    companion object {
        // 카드 그림과 이름. 어르신이 바로 알아보는 물건과 동물, 과일로 (나중에 game_ 아이콘으로 바꿀 수 있다)
        private val PICS = listOf(
            "🍎" to "사과", "🍌" to "바나나", "🍇" to "포도", "🍉" to "수박", "🍓" to "딸기", "🍊" to "귤",
            "🥕" to "당근", "🌽" to "옥수수", "🍞" to "빵", "🥚" to "달걀", "🍵" to "차",
            "🐶" to "강아지", "🐱" to "고양이", "🐰" to "토끼", "🐟" to "물고기", "🐔" to "닭", "🐸" to "개구리",
            "🌻" to "해바라기", "🌷" to "튤립", "🌲" to "나무", "☀️" to "해", "🌙" to "달", "⭐" to "별",
            "☂️" to "우산", "👓" to "안경", "⌚" to "시계", "🔑" to "열쇠", "✂️" to "가위", "📖" to "책",
            "🏠" to "집", "🚗" to "자동차", "🚲" to "자전거", "⚽" to "공", "🎈" to "풍선", "🧦" to "양말"
        )
    }
}

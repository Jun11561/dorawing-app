package com.team3.dorawing.ui.game

import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGameCalcBinding

// 계산력 게임: 시장 보기. 얼마예요(합계, 거스름돈, 곱셈), 돈 내기(지폐와 동전 누르기), 몇 시에 도착(시계)
class CalcGameActivity : BaseGameActivity() {
    private lateinit var binding: ActivityGameCalcBinding
    private var kind = ""
    private var buttons = listOf<MaterialButton>()

    // 얼마예요
    private var sum = CalcRules.SumProblem(emptyList(), null, 0, emptyList())

    // 돈 내기
    private var target = 0
    private var paid = 0

    // 몇 시에 도착
    private var time = CalcRules.TimeProblem(0, 0, 0, emptyList())

    override fun contentLayout() = R.layout.activity_game_calc

    override fun bindContent(content: View) {
        binding = ActivityGameCalcBinding.bind(content)
        binding.btnReset.setOnClickListener { resetPaid() }
        binding.btnPay.setOnClickListener { checkPaid() }
    }

    override fun makeProblems(level: Int) = CalcRules.problems(level)

    override fun showProblem(kind: String, retry: Boolean) {
        this.kind = kind
        binding.boxItems.removeAllViews()
        binding.listChoices.removeAllViews()
        binding.boxMoney.visibility = View.GONE
        binding.boxItems.visibility = View.GONE
        when (kind) {
            CalcRules.SUM -> startSum()
            CalcRules.PAY -> startPay()
            else -> startTime()
        }
    }

    // ---------- 얼마예요 ----------

    private fun startSum() {
        sum = CalcRules.makeSum(level)
        binding.boxItems.visibility = View.VISIBLE
        for ((item, count) in sum.items) addItem(item, count)

        val question = when {
            sum.pay != null -> getString(R.string.game_calc_change_q, won(sum.pay!!))
            else -> getString(R.string.game_calc_sum_q)
        }
        binding.tvQuestion.text = question
        buttons = addChoices(binding.listChoices, sum.choices.map { won(it) }, perRow = 2) { i, _ ->
            chooseSum(i)
        }

        // 읽어 줄 때는 물건과 가격을 하나씩 말한다
        val spoken = sum.items.joinToString(", ") { (item, count) ->
            if (count > 1) "${item.name} ${won(item.price)}짜리 ${count}개" else "${item.name} ${won(item.price)}"
        }
        setGuide(question, "$spoken. $question")
    }

    private fun chooseSum(i: Int) {
        val pick = sum.choices[i]
        if (pick == sum.answer) {
            markChoice(buttons[i], true)
            answered(true)
        } else {
            markChoice(buttons[sum.choices.indexOf(sum.answer)], true)
            answered(false, getString(R.string.game_calc_wrong, won(sum.answer)))
        }
    }

    // 물건 하나: 그림, 이름, 가격표, 개수
    private fun addItem(item: CalcRules.Item, count: Int) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        box.addView(TextView(this).apply {
            text = item.pic
            textSize = 40f
            gravity = Gravity.CENTER
        })
        box.addView(TextView(this).apply {
            text = if (count > 1) getString(R.string.game_calc_item_count, item.name, count) else item.name
            gravity = Gravity.CENTER
        })
        box.addView(TextView(this).apply {
            text = won(item.price)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_chip)
            setPadding(16, 8, 16, 8)
            setTextColor(ContextCompat.getColor(context, R.color.green_dark))
        })
        binding.boxItems.addView(box)
    }

    // ---------- 돈 내기 ----------

    private fun startPay() {
        target = CalcRules.makePay(level)
        paid = 0
        binding.boxMoney.visibility = View.VISIBLE
        val question = getString(R.string.game_calc_pay_q, won(target))
        binding.tvQuestion.text = question

        // 지폐 한 줄, 동전 한 줄
        binding.rowBills.removeAllViews()
        binding.rowCoins.removeAllViews()
        for (money in CalcRules.moneyFor(level)) {
            val button = MaterialButton(ContextThemeWrapper(this, R.style.ThemeOverlay_Dorawing_SecondaryButton))
            button.text = moneyName(money)
            button.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = 8 }
            button.setOnClickListener { addMoney(money) }
            (if (money >= 1000) binding.rowBills else binding.rowCoins).addView(button)
        }
        binding.rowCoins.visibility = if (binding.rowCoins.childCount == 0) View.GONE else View.VISIBLE
        showPaid()
        setGuide(question, "$question " + getString(R.string.game_calc_pay_speech))
    }

    private fun addMoney(money: Int) {
        paid += money
        ding()
        showPaid()
    }

    private fun resetPaid() {
        paid = 0
        showPaid()
    }

    private fun showPaid() {
        binding.tvPaid.text = getString(R.string.game_calc_paid, won(paid))
    }

    private fun checkPaid() {
        if (paid == target) {
            answered(true)
        } else {
            val message = if (paid > target) {
                getString(R.string.game_calc_pay_over, won(paid - target))
            } else {
                getString(R.string.game_calc_pay_short, won(target - paid))
            }
            answered(false, message)
        }
    }

    // ---------- 몇 시에 도착 ----------

    private fun startTime() {
        time = CalcRules.makeTime(level)
        val question = getString(
            R.string.game_calc_time_q,
            CalcRules.timeText(time.start),
            CalcRules.takesText(time.takes)
        )
        binding.tvQuestion.text = question
        buttons = addChoices(
            binding.listChoices,
            time.choices.map { CalcRules.clockEmoji(it) + "  " + CalcRules.timeText(it) },
            perRow = 2
        ) { i, _ -> chooseTime(i) }
        setGuide(question)
    }

    private fun chooseTime(i: Int) {
        if (time.choices[i] == time.answer) {
            markChoice(buttons[i], true)
            answered(true)
        } else {
            markChoice(buttons[time.choices.indexOf(time.answer)], true)
            answered(false, getString(R.string.game_calc_time_wrong, CalcRules.timeText(time.answer)))
        }
    }

    // ---------- 힌트 ----------

    // 답은 말하지 않고 계산하는 방법만 알려 준다
    override fun onHint() {
        val text = when (kind) {
            CalcRules.SUM -> {
                val parts = sum.items.joinToString(" 더하기 ") { (item, count) ->
                    if (count > 1) "${won(item.price)} 곱하기 $count" else won(item.price)
                }
                if (sum.pay != null) getString(R.string.game_calc_hint_change, won(sum.pay!!), parts)
                else getString(R.string.game_calc_hint_sum, parts)
            }
            CalcRules.PAY -> {
                val left = target - paid
                if (left > 0) getString(R.string.game_calc_hint_pay, won(left))
                else getString(R.string.game_calc_hint_pay_over)
            }
            else -> getString(R.string.game_calc_hint_time, CalcRules.timeText(time.start), CalcRules.takesText(time.takes))
        }
        say(text)
    }

    private fun won(amount: Int): String = String.format("%,d원", amount)

    private fun moneyName(money: Int): String = when (money) {
        10000 -> "만 원"
        5000 -> "오천 원"
        1000 -> "천 원"
        500 -> "오백 원"
        else -> "백 원"
    }
}

package com.team3.dorawing.ui.diagnosis

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityDiagnosisReviewBinding
import com.team3.dorawing.db.AppDatabase
import com.team3.dorawing.db.DiagnosisResult
import com.team3.dorawing.db.LevelState
import com.team3.dorawing.game.Difficulty
import com.team3.dorawing.game.GameType
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar
import kotlinx.coroutines.launch

// 15문항의 답을 한눈에 보고 고친 뒤 결과를 본다. 결과 저장은 여기서만 한다
class DiagnosisReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDiagnosisReviewBinding
    private lateinit var questions: List<String>
    private lateinit var answers: IntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosisReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        questions = DiagnosisQuestionActivity.loadQuestions(this)
        answers = intent.getIntArrayExtra("answers") ?: IntArray(questions.size) { -1 }

        val guide = getString(R.string.review_guide)
        TopBar.setup(
            this, binding.topBar, getString(R.string.title_diagnosis_review),
            onHome = { DiagnosisQuestionActivity.confirmQuit(this) { TopBar.goHome(this) } }
        ) { guide }
        Speaker.speak(this, guide)

        questions.forEachIndexed { i, question -> addRow(i, question) }
        binding.btnResult.setOnClickListener { showResult() }
    }

    private fun addRow(i: Int, question: String) {
        val gap = resources.getDimensionPixelSize(R.dimen.gap)
        val outValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)

        // 글자를 키운 폰에서도 넉넉하게 문항 아래에 답을 쌓는다
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = resources.getDimensionPixelSize(R.dimen.button_height)
            setPadding(0, gap, 0, gap)
            setBackgroundResource(outValue.resourceId)
            // 줄을 누르면 그 문항을 고치러 간다
            setOnClickListener { edit(i, false) }
        }

        val tvQuestion = TextView(this).apply {
            text = getString(R.string.review_row, i + 1, question)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_button))
        }
        val answered = answers[i] in 0..2
        val tvAnswer = TextView(this).apply {
            text = if (answered) getString(answerText(answers[i])) else getString(R.string.review_no_answer)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_button))
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, if (answered) R.color.green_dark else R.color.text_sub))
            setPadding(0, gap / 2, 0, 0)
        }
        row.addView(tvQuestion, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        row.addView(tvAnswer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        binding.answerList.addView(row)

        // 줄 사이 구분선
        val line = View(this).apply { setBackgroundColor(ContextCompat.getColor(context, R.color.green_light)) }
        binding.answerList.addView(line, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2))
    }

    private fun answerText(score: Int): Int = when (score) {
        0 -> R.string.diag_answer_0
        1 -> R.string.diag_answer_1
        else -> R.string.diag_answer_2
    }

    // 문항 화면으로 돌아가 그 문항을 보여 준다
    private fun edit(i: Int, missing: Boolean) {
        setResult(RESULT_EDIT, Intent().putExtra("index", i).putExtra("missing", missing))
        finish()
    }

    private fun showResult() {
        // 답하지 않은 문항이 있으면 그 문항으로
        val empty = answers.indexOfFirst { it !in 0..2 }
        if (empty >= 0) {
            edit(empty, true)
            return
        }

        binding.btnResult.isEnabled = false
        val total = DiagnosisScore.total(answers.toList())
        val result = DiagnosisResult(
            takenAt = System.currentTimeMillis(),
            totalScore = total,
            isSuspected = DiagnosisScore.isSuspected(total)
        )
        lifecycleScope.launch {
            val db = AppDatabase.get(this@DiagnosisReviewActivity)
            db.diagnosisDao().insert(result)
            // "자가진단을 다시 해 볼까요?" 알림은 했으니 닫는다
            db.changeAlertDao().dismissAll()

            // 아직 레벨이 없는 영역만 시작 레벨을 넣는다. 이미 훈련한 영역은 그대로
            val startLevel = Difficulty.startLevel(result)
            var newAreas = 0
            for (type in GameType.entries) {
                if (db.levelStateDao().get(type.name) == null) {
                    db.levelStateDao().insertIfEmpty(LevelState(type.name, startLevel, 0, 0))
                    newAreas++
                }
            }

            val intent = Intent(this@DiagnosisReviewActivity, DiagnosisResultActivity::class.java)
                .putExtra("score", total)
                .putExtra("maxScore", answers.size * 2)
                .putExtra("startLevel", if (newAreas > 0) startLevel else 0)
            startActivity(intent)
            setResult(RESULT_DONE)
            finish()
        }
    }

    companion object {
        const val RESULT_EDIT = Activity.RESULT_FIRST_USER
        const val RESULT_DONE = Activity.RESULT_FIRST_USER + 1
    }
}

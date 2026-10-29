package com.team3.dorawing.ui.diagnosis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityDiagnosisQuestionBinding
import com.team3.dorawing.db.AppDatabase
import com.team3.dorawing.db.DiagnosisResult
import com.team3.dorawing.db.LevelState
import com.team3.dorawing.game.Difficulty
import com.team3.dorawing.game.GameType
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar
import kotlinx.coroutines.launch
import org.json.JSONArray

// 한 화면에 한 문항. 답을 고르면 바로 다음 문항으로 넘어간다
class DiagnosisQuestionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDiagnosisQuestionBinding
    private lateinit var questions: List<String>
    private lateinit var answers: IntArray // 아직 안 고른 문항은 -1
    private var index = 0
    private var busy = false

    private val answerButtons by lazy { listOf(binding.btnAnswer0, binding.btnAnswer1, binding.btnAnswer2) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosisQuestionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        questions = loadQuestions(this)
        answers = savedInstanceState?.getIntArray("answers") ?: IntArray(questions.size) { -1 }
        index = savedInstanceState?.getInt("index") ?: 0

        TopBar.setup(this, binding.topBar, getString(R.string.title_diagnosis_question)) { questionSpeech() }

        answerButtons.forEachIndexed { score, button ->
            button.setOnClickListener { choose(score) }
        }
        binding.btnPrev.setOnClickListener { goBack() }
        binding.btnListen.setOnClickListener { Speaker.speak(this, questionSpeech()) }
        // 뒤로 가기는 이전 문항으로
        onBackPressedDispatcher.addCallback(this) { goBack() }

        showQuestion()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray("answers", answers)
        outState.putInt("index", index)
    }

    private fun showQuestion() {
        binding.tvProgress.text = getString(R.string.diag_progress, index + 1, questions.size)
        binding.tvQuestion.text = questions[index]
        binding.btnPrev.visibility = if (index > 0) View.VISIBLE else View.INVISIBLE
        // 이전 문항으로 돌아왔으면 골랐던 답을 표시
        answerButtons.forEachIndexed { score, button -> paint(button, answers[index] == score) }
        Speaker.speak(this, questionSpeech())
    }

    private fun questionSpeech(): String {
        val text = getString(R.string.diag_question_speech, index + 1, questions[index])
        // 처음 문항에서만 고르는 방법을 함께 읽는다
        return if (index == 0) text + " " + getString(R.string.diag_choice_speech) else text
    }

    private fun choose(score: Int) {
        if (busy) return
        busy = true
        answers[index] = score
        answerButtons.forEachIndexed { i, button -> paint(button, i == score) }

        // 고른 답이 잠깐 보이도록 조금 있다가 넘어간다
        binding.root.postDelayed({
            busy = false
            if (index < questions.size - 1) {
                index++
                showQuestion()
            } else {
                finishTest()
            }
        }, 400)
    }

    private fun goBack() {
        if (busy) return
        if (index > 0) {
            index--
            showQuestion()
        } else {
            finish()
        }
    }

    private fun finishTest() {
        busy = true
        val total = DiagnosisScore.total(answers.toList())
        val result = DiagnosisResult(
            takenAt = System.currentTimeMillis(),
            totalScore = total,
            isSuspected = DiagnosisScore.isSuspected(total)
        )
        lifecycleScope.launch {
            val db = AppDatabase.get(this@DiagnosisQuestionActivity)
            db.diagnosisDao().insert(result)

            // 아직 레벨이 없는 영역만 시작 레벨을 넣는다. 이미 훈련한 영역은 그대로
            val startLevel = Difficulty.startLevel(result)
            var newAreas = 0
            for (type in GameType.entries) {
                if (db.levelStateDao().get(type.name) == null) {
                    db.levelStateDao().insertIfEmpty(LevelState(type.name, startLevel, 0, 0))
                    newAreas++
                }
            }

            val intent = Intent(this@DiagnosisQuestionActivity, DiagnosisResultActivity::class.java)
                .putExtra("score", total)
                .putExtra("maxScore", questions.size * 2)
                .putExtra("startLevel", if (newAreas > 0) startLevel else 0)
            startActivity(intent)
            finish()
        }
    }

    // 고른 답은 진한 초록, 나머지는 연한 초록
    private fun paint(button: MaterialButton, selected: Boolean) {
        val back = if (selected) R.color.green_dark else R.color.green_light
        val text = if (selected) R.color.white else R.color.green_dark
        button.backgroundTintList = ContextCompat.getColorStateList(this, back)
        button.setTextColor(ContextCompat.getColor(this, text))
    }

    companion object {
        // res/raw/kdsq_questions.json: 문항 문자열 배열
        fun loadQuestions(context: Context): List<String> {
            val text = context.resources.openRawResource(R.raw.kdsq_questions).bufferedReader().use { it.readText() }
            val array = JSONArray(text)
            return (0 until array.length()).map { array.getString(it) }
        }
    }
}

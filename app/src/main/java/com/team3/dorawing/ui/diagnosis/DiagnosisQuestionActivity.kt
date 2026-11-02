package com.team3.dorawing.ui.diagnosis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityDiagnosisQuestionBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar
import org.json.JSONArray

// 한 화면에 한 문항. 답을 고르면 1초 뒤 다음 문항으로, 마지막 문항 뒤에는 답 확인 화면으로 간다.
// 답은 이 화면이 들고 있고, 결과 저장은 답 확인 화면의 "결과 보기"에서만 한다
class DiagnosisQuestionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDiagnosisQuestionBinding
    private lateinit var questions: List<String>
    private lateinit var answers: IntArray // 아직 안 고른 문항은 -1
    private var index = 0
    private var editing = false // 답 확인 화면에서 고치러 왔을 때
    private var missing = false // 답하지 않은 문항으로 왔을 때
    private var busy = false

    private val answerButtons by lazy { listOf(binding.btnAnswer0, binding.btnAnswer1, binding.btnAnswer2) }
    private val goNext = Runnable { next() }

    private val reviewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            DiagnosisReviewActivity.RESULT_DONE -> finish()
            DiagnosisReviewActivity.RESULT_EDIT -> {
                val data = result.data
                index = data?.getIntExtra("index", 0) ?: 0
                missing = data?.getBooleanExtra("missing", false) ?: false
                editing = true
                showQuestion()
            }
            // 답 확인 화면에서 뒤로 가기: 마지막 문항으로 돌아온다
            else -> {
                index = questions.size - 1
                editing = false
                missing = false
                showQuestion()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosisQuestionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        questions = loadQuestions(this)
        // 화면이 돌아가거나 앱이 잠시 꺼졌다 켜져도 고른 답이 남게
        answers = savedInstanceState?.getIntArray("answers") ?: IntArray(questions.size) { -1 }
        index = savedInstanceState?.getInt("index") ?: 0
        editing = savedInstanceState?.getBoolean("editing") ?: false
        missing = savedInstanceState?.getBoolean("missing") ?: false

        TopBar.setup(
            this, binding.topBar, getString(R.string.title_diagnosis_question),
            onHome = { confirmQuit(this) { TopBar.goHome(this) } }
        ) { questionSpeech() }

        answerButtons.forEachIndexed { score, button ->
            button.setOnClickListener { choose(score) }
        }
        binding.btnPrev.setOnClickListener { goBack() }
        binding.btnListen.setOnClickListener { Speaker.speak(this, questionSpeech()) }
        onBackPressedDispatcher.addCallback(this) { goBack() }

        showQuestion()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray("answers", answers)
        outState.putInt("index", index)
        outState.putBoolean("editing", editing)
        outState.putBoolean("missing", missing)
    }

    override fun onDestroy() {
        binding.root.removeCallbacks(goNext)
        super.onDestroy()
    }

    private fun showQuestion() {
        busy = false
        binding.tvProgress.text = getString(R.string.diag_progress, index + 1, questions.size)
        binding.tvQuestion.text = questions[index]
        binding.tvNotice.visibility = if (missing) View.VISIBLE else View.GONE
        // 고치러 왔을 때는 이전 문항 대신 답 확인으로 돌아가는 버튼
        binding.btnPrev.setText(if (editing) R.string.diag_back_review else R.string.diag_prev)
        binding.btnPrev.visibility = if (index > 0 || editing) View.VISIBLE else View.INVISIBLE
        // 전에 골랐던 답을 표시
        answerButtons.forEachIndexed { score, button -> paint(button, answers[index] == score) }
        Speaker.speak(this, questionSpeech())
    }

    private fun questionSpeech(): String {
        var text = getString(R.string.diag_question_speech, index + 1, questions[index])
        if (missing) text = getString(R.string.diag_answer_please) + ". " + text
        // 처음 문항에서만 고르는 방법을 함께 읽는다
        if (index == 0 && !editing) text += " " + getString(R.string.diag_choice_speech)
        return text
    }

    private fun choose(score: Int) {
        if (busy) return
        busy = true
        answers[index] = score
        answerButtons.forEachIndexed { i, button -> paint(button, i == score) }
        Speaker.stop()
        // 고른 답이 잠깐 보이도록 1초 뒤에 넘어간다
        binding.root.postDelayed(goNext, 1000)
    }

    private fun next() {
        if (editing || index == questions.size - 1) {
            openReview()
        } else {
            index++
            showQuestion()
        }
    }

    private fun goBack() {
        if (busy) return
        when {
            editing -> openReview()
            index > 0 -> {
                index--
                showQuestion()
            }
            else -> confirmQuit(this) { finish() }
        }
    }

    private fun openReview() {
        busy = false
        editing = false
        missing = false
        val intent = Intent(this, DiagnosisReviewActivity::class.java).putExtra("answers", answers)
        reviewLauncher.launch(intent)
    }

    // 답 버튼은 모두 같은 색, 고른 답만 진한 초록
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

        // 푸는 도중 나가려고 할 때 묻는다
        fun confirmQuit(activity: AppCompatActivity, onQuit: () -> Unit) {
            val title = activity.getString(R.string.diag_quit_title)
            val message = activity.getString(R.string.diag_quit_message)
            MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.diag_quit_yes) { _, _ -> onQuit() }
                .setNegativeButton(R.string.diag_quit_no, null)
                .show()
            Speaker.speak(activity, "$title $message")
        }
    }
}

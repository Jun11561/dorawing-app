package com.team3.dorawing.ui.game

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGameBaseBinding
import com.team3.dorawing.game.GameRecorder
import com.team3.dorawing.game.GameType
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar
import kotlinx.coroutines.launch

// 게임 공통 틀. 게임 화면은 이 클래스를 상속해서 문제를 보여 주고, 답이 나오면 answered만 부른다.
// 진행 표시, 맞음/틀림 안내, 힌트 버튼, 다시 해 보기, 세트가 끝났을 때 기록 저장과 결과 화면 이동은 여기서 한다
abstract class BaseGameActivity : AppCompatActivity() {
    protected lateinit var base: ActivityGameBaseBinding
    protected lateinit var gameType: GameType
    protected var level = 1
    // 게임 화면이 시간을 두고 할 일을 걸어 두는 곳. 화면이 닫히면 모두 지운다
    protected val handler = Handler(Looper.getMainLooper())

    // 이번 세트의 문제 유형 순서. 틀려서 다시 할 때는 같은 유형이 바로 뒤에 끼어든다
    private val problems = mutableListOf<String>()
    private var index = 0
    private var retrying = false // 지금 문제가 다시 하기인지
    private var waiting = false  // 답을 받고 다음으로 넘어가는 중
    private var hintUsed = false // 힌트는 문제마다 한 번만. 계속 누르면 힌트로만 맞혀서 정답률이 실제보다 높아진다
    private var startedAt = 0L
    private val reactions = mutableListOf<Long>()
    // 유형별 맞힌 수와 전체 수. 결과 화면의 "오늘 가장 잘한 유형"에 쓴다
    private val correctByKind = linkedMapOf<String, Int>()
    private val totalByKind = linkedMapOf<String, Int>()
    private var tone: ToneGenerator? = null
    private var currentSpeech = ""
    private val goNext = Runnable { next() }

    // 게임 화면이 정하는 것
    abstract fun contentLayout(): Int                   // gameArea에 넣을 레이아웃
    abstract fun bindContent(content: View)             // 넣은 레이아웃의 뷰를 잡아 둔다
    abstract fun makeProblems(level: Int): List<String> // 세트의 문제 유형 순서 (한글 이름)
    abstract fun showProblem(kind: String, retry: Boolean)
    abstract fun onHint()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = GameType.fromName(intent.getStringExtra("gameType") ?: "")
        if (type == null) {
            finish()
            return
        }
        gameType = type
        level = intent.getIntExtra("level", 1)

        base = ActivityGameBaseBinding.inflate(layoutInflater)
        setContentView(base.root)
        val content = layoutInflater.inflate(contentLayout(), base.gameArea, false)
        base.gameArea.addView(content)
        bindContent(content)

        val title = getString(R.string.game_title_play, gameType.label, level)
        TopBar.setup(this, base.topBar, title, onHome = { confirmQuit { TopBar.goHome(this) } }) { currentSpeech }
        onBackPressedDispatcher.addCallback(this) { confirmQuit { finish() } }

        base.btnHint.setOnClickListener {
            if (waiting || hintUsed) return@setOnClickListener
            hintUsed = true
            base.btnHint.isEnabled = false
            base.btnHint.setText(R.string.game_hint_used)
            onHint()
        }
        base.btnNext.setOnClickListener {
            handler.removeCallbacks(goNext)
            next()
        }

        tone = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: RuntimeException) {
            null
        }

        problems.addAll(makeProblems(level))
        startProblem()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        tone?.release()
        super.onDestroy()
    }

    // 문제 안내 문장. 화면에 보여 주고 읽어 준다. 듣기 버튼을 누르면 이 문장을 다시 읽는다
    protected fun setGuide(text: String, speech: String = text) {
        base.tvGuide.text = text
        currentSpeech = speech
        Speaker.speak(this, speech)
    }

    // 화면 글은 그대로 두고 읽기만 한다
    protected fun say(text: String) {
        Speaker.speak(this, text)
    }

    // 짝을 찾았을 때처럼 작은 성공에 짧은 소리
    protected fun ding() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    }

    private fun startProblem() {
        waiting = false
        val kind = problems[index]
        base.tvKind.text = kind
        base.tvProgress.text = getString(R.string.game_progress, index + 1, problems.size)
        base.progressBar.max = problems.size
        base.progressBar.progress = index + 1
        base.tvFeedback.visibility = View.GONE
        base.btnNext.visibility = View.GONE
        hintUsed = false
        base.btnHint.isEnabled = true
        base.btnHint.setText(R.string.game_hint)
        base.btnHint.visibility = View.VISIBLE
        startedAt = System.currentTimeMillis()
        showProblem(kind, retrying)
    }

    // 게임 화면이 답이 나왔을 때 한 번 부른다.
    // message: 틀렸을 때 보여 줄 문장 (정답을 알려 주는 말). canRetry: 같은 유형으로 한 번 더 기회를 줄지
    protected fun answered(correct: Boolean, message: String? = null, canRetry: Boolean = true) {
        if (waiting) return
        waiting = true
        reactions.add(System.currentTimeMillis() - startedAt)
        val kind = problems[index]
        totalByKind[kind] = (totalByKind[kind] ?: 0) + 1
        if (correct) correctByKind[kind] = (correctByKind[kind] ?: 0) + 1

        base.btnHint.visibility = View.GONE
        base.tvFeedback.visibility = View.VISIBLE
        if (correct) {
            val praise = getString(R.string.game_correct)
            base.tvFeedback.text = praise
            tone?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
            Speaker.speak(this, praise)
            retrying = false
            handler.postDelayed(goNext, 1500)
        } else {
            // 틀려도 빨간 X 대신 정답을 보여 주고 "다시 해 볼까요?"로 안내한다. 다시 하기는 한 번만
            val text = message ?: getString(R.string.game_wrong)
            val again = canRetry && !retrying
            if (again) problems.add(index + 1, kind)
            retrying = again
            base.tvFeedback.text = text
            base.btnNext.setText(if (again) R.string.game_retry else R.string.game_next)
            base.btnNext.visibility = View.VISIBLE
            Speaker.speak(this, text + " " + getString(if (again) R.string.game_retry else R.string.game_next))
        }
    }

    private fun next() {
        if (!waiting) return
        index++
        if (index >= problems.size) finishSet() else startProblem()
    }

    // 세트 끝. 저장은 GameRecorder.finish 한 번으로 끝나고, 요약을 결과 화면에 넘긴다
    private fun finishSet() {
        val correct = correctByKind.values.sum()
        val total = totalByKind.values.sum()
        val avg = if (reactions.isEmpty()) 0L else reactions.average().toLong()
        // 유형별 정답률이 가장 높은 유형. 하나도 못 맞혔으면 없음
        val best = totalByKind.keys
            .maxByOrNull { (correctByKind[it] ?: 0).toFloat() / totalByKind.getValue(it) }
            ?.takeIf { (correctByKind[it] ?: 0) > 0 }
        lifecycleScope.launch {
            val summary = GameRecorder.finish(this@BaseGameActivity, gameType, level, correct, total, avg)
            startActivity(
                Intent(this@BaseGameActivity, GameResultActivity::class.java)
                    .putExtra("summary", summary)
                    .putExtra("gameType", gameType.name)
                    .putExtra("bestKind", best)
            )
            finish()
        }
    }

    // 하는 도중 나가려고 할 때 묻는다
    private fun confirmQuit(onQuit: () -> Unit) {
        val title = getString(R.string.game_quit_title)
        val message = getString(R.string.game_quit_message)
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.game_quit_yes) { _, _ -> onQuit() }
            .setNegativeButton(R.string.game_quit_no, null)
            .show()
        Speaker.speak(this, "$title $message")
    }
}

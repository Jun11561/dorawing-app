package com.team3.dorawing.ui.diagnosis

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityDiagnosisResultBinding
import com.team3.dorawing.db.DiagnosisResult
import com.team3.dorawing.ui.game.GameSelectActivity
import com.team3.dorawing.util.Dates
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

// 결과를 보여 주기만 한다. 저장은 답 확인 화면에서 한다.
// 지난 결과를 다시 볼 때도 이 화면을 쓴다 (pastIntent)
class DiagnosisResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDiagnosisResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosisResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val score = intent.getIntExtra("score", 0)
        val maxScore = intent.getIntExtra("maxScore", DiagnosisScore.MAX_SCORE)
        val startLevel = intent.getIntExtra("startLevel", 0)
        val takenAt = intent.getLongExtra("takenAt", 0)
        val past = takenAt > 0
        val suspected = DiagnosisScore.isSuspected(score)

        if (past) {
            binding.tvDate.visibility = View.VISIBLE
            binding.tvDate.text = getString(R.string.result_past_date, Dates.koreanDay(takenAt))
        }
        binding.tvHeadline.setText(if (suspected) R.string.result_suspected_title else R.string.result_normal_title)
        val judge = getString(if (suspected) R.string.result_suspected else R.string.result_normal)
        binding.tvJudge.text = getString(R.string.result_judge, judge, score, maxScore)
        binding.tvDetail.setText(if (suspected) R.string.result_suspected_detail else R.string.result_normal_detail)

        // 지난 결과에는 시작 단계 안내가 필요 없다
        if (past) {
            binding.tvLevel.visibility = View.GONE
        } else {
            binding.tvLevel.text = if (startLevel > 0) getString(R.string.result_start_level, startLevel)
            else getString(R.string.result_keep_level)
        }

        // 6점 이상일 때만 전화 버튼. ACTION_DIAL은 전화 앱을 열기만 해서 권한이 필요 없다
        binding.callBox.visibility = if (suspected) View.VISIBLE else View.GONE
        binding.btnCall.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_DIAL, "tel:18999988".toUri()))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, R.string.result_no_phone, Toast.LENGTH_LONG).show()
            }
        }
        // 전화 버튼이 있으면 그쪽을 먼저 보도록 훈련 버튼은 보조 색으로
        if (suspected) {
            binding.btnGame.backgroundTintList = ContextCompat.getColorStateList(this, R.color.white)
            binding.btnGame.setTextColor(ContextCompat.getColor(this, R.color.green_dark))
            binding.btnGame.strokeColor = ContextCompat.getColorStateList(this, R.color.green_mid)
            binding.btnGame.strokeWidth = resources.getDimensionPixelSize(R.dimen.button_stroke)
        }
        // 지난 결과를 다시 볼 때는 훈련 안내가 필요 없다
        if (past) binding.btnGame.visibility = View.GONE
        binding.btnGame.setOnClickListener {
            startActivity(Intent(this, GameSelectActivity::class.java))
            finish()
        }

        val parts = mutableListOf<CharSequence>()
        if (past) parts += binding.tvDate.text
        parts += listOf(binding.tvHeadline.text, binding.tvJudge.text, binding.tvDetail.text)
        if (!past) parts += binding.tvLevel.text
        parts += binding.tvNote.text
        if (suspected) parts += listOf(binding.btnCall.text, binding.tvCallHelp.text)
        val speech = parts.joinToString(". ")

        TopBar.setup(this, binding.topBar, getString(R.string.title_diagnosis_result)) { speech }
        Speaker.speak(this, speech)
    }

    companion object {
        // 지난 결과 다시 보기. 저장이나 시작 레벨 설정은 하지 않는다
        fun pastIntent(context: Context, result: DiagnosisResult): Intent =
            Intent(context, DiagnosisResultActivity::class.java)
                .putExtra("score", result.totalScore)
                .putExtra("maxScore", DiagnosisScore.MAX_SCORE)
                .putExtra("takenAt", result.takenAt)
    }
}

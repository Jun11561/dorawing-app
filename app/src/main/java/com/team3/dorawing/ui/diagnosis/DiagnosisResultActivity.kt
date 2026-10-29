package com.team3.dorawing.ui.diagnosis

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityDiagnosisResultBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

// 결과는 DiagnosisQuestionActivity에서 저장하고 여기서는 보여 주기만 한다
class DiagnosisResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDiagnosisResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosisResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val score = intent.getIntExtra("score", 0)
        val maxScore = intent.getIntExtra("maxScore", 30)
        val startLevel = intent.getIntExtra("startLevel", 0)
        val suspected = DiagnosisScore.isSuspected(score)

        binding.tvStage.setText(if (suspected) R.string.result_suspected else R.string.result_normal)
        binding.tvScore.text = getString(R.string.result_score, score, maxScore)
        binding.tvDetail.setText(if (suspected) R.string.result_suspected_detail else R.string.result_normal_detail)
        binding.tvLevel.text = if (startLevel > 0) getString(R.string.result_start_level, startLevel)
        else getString(R.string.result_keep_level)
        if (suspected) {
            binding.cardResult.setCardBackgroundColor(ContextCompat.getColor(this, R.color.pink_light))
            binding.tvStage.setTextColor(ContextCompat.getColor(this, R.color.coral_dark))
        }

        // 6점 이상일 때만 전화 버튼. ACTION_DIAL은 전화 앱을 열기만 해서 권한이 필요 없다
        binding.btnCall.visibility = if (suspected) View.VISIBLE else View.GONE
        binding.btnCall.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:18999988")))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, R.string.result_no_phone, Toast.LENGTH_LONG).show()
            }
        }
        binding.btnHomeBottom.setOnClickListener { finish() }

        val speech = listOf(
            binding.tvStage.text, binding.tvScore.text, binding.tvDetail.text,
            binding.tvLevel.text, binding.tvNote.text
        ).joinToString(". ")
        TopBar.setup(this, binding.topBar, getString(R.string.title_diagnosis_result)) { speech }
        Speaker.speak(this, speech)
    }
}

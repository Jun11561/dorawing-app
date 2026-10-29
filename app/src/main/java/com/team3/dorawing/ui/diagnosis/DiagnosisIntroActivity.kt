package com.team3.dorawing.ui.diagnosis

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityDiagnosisIntroBinding
import com.team3.dorawing.db.AppDatabase
import com.team3.dorawing.util.Dates
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar
import kotlinx.coroutines.launch

class DiagnosisIntroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDiagnosisIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosisIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val count = DiagnosisQuestionActivity.loadQuestions(this).size
        binding.tvDetail.text = getString(R.string.diag_intro_detail, count)

        val speech = getString(R.string.diag_intro_guide) + " " + binding.tvDetail.text
        TopBar.setup(this, binding.topBar, getString(R.string.title_diagnosis_intro)) { speech }
        Speaker.speak(this, speech)

        binding.btnStart.setOnClickListener {
            startActivity(Intent(this, DiagnosisQuestionActivity::class.java))
            finish()
        }
    }

    // 결과를 보고 돌아올 수 있으니 들어올 때마다 지난 기록을 다시 읽는다
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val last = AppDatabase.get(this@DiagnosisIntroActivity).diagnosisDao().last() ?: return@launch
            binding.tvLast.visibility = View.VISIBLE
            binding.tvLast.text = getString(R.string.diag_last_date, Dates.koreanDay(last.takenAt))
            binding.btnLast.visibility = View.VISIBLE
            binding.btnLast.setOnClickListener {
                startActivity(DiagnosisResultActivity.pastIntent(this@DiagnosisIntroActivity, last))
            }
            binding.btnStart.setText(R.string.diag_restart)
        }
    }
}

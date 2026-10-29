package com.team3.dorawing.ui.diagnosis

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityDiagnosisIntroBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

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
}

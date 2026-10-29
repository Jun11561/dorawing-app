package com.team3.dorawing.ui.diagnosis

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityDiagnosisQuestionBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

class DiagnosisQuestionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDiagnosisQuestionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosisQuestionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = getString(R.string.title_diagnosis_question)
        TopBar.setup(this, binding.topBar, title)
        Speaker.speak(this, title)
    }
}

package com.team3.dorawing.ui.home

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityNameInputBinding
import com.team3.dorawing.util.Prefs
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

// 처음 실행 때만 홈에서 연다
class NameInputActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNameInputBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNameInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val guide = getString(R.string.name_guide)
        // 홈 버튼 대신 건너뛰기로 홈에 간다
        TopBar.setup(this, binding.topBar, getString(R.string.title_name_input), showHome = false) { guide }
        Speaker.speak(this, guide)

        binding.btnOk.setOnClickListener { save() }
        binding.btnSkip.setOnClickListener { skip() }
        binding.etName.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) save()
            true
        }
        // 뒤로 가기도 건너뛰기와 같다
        onBackPressedDispatcher.addCallback(this) { skip() }
    }

    private fun save() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            skip()
            return
        }
        Prefs.setName(this, name)
        finish()
    }

    private fun skip() {
        Prefs.setName(this, getString(R.string.name_default))
        finish()
    }
}

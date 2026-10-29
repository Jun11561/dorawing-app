package com.team3.dorawing.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.team3.dorawing.BuildConfig
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityHomeBinding
import com.team3.dorawing.ui.diagnosis.DiagnosisIntroActivity
import com.team3.dorawing.ui.diagnosis.DiagnosisQuestionActivity
import com.team3.dorawing.ui.diagnosis.DiagnosisResultActivity
import com.team3.dorawing.ui.game.GameSelectActivity
import com.team3.dorawing.ui.guardian.GuardianCodeActivity
import com.team3.dorawing.ui.guardian.GuardianConsentActivity
import com.team3.dorawing.ui.guardian.GuardianEnterCodeActivity
import com.team3.dorawing.ui.guardian.GuardianHomeActivity
import com.team3.dorawing.ui.guardian.GuardianRoleSelectActivity
import com.team3.dorawing.ui.record.RecordActivity
import com.team3.dorawing.ui.settings.SettingsActivity
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = getString(R.string.title_home)
        TopBar.setup(this, binding.topBar, title, showHome = false)
        Speaker.speak(this, "두로잉에 오신 것을 환영해요")

        // 임시 버튼 목록. 세션 2에서 홈 화면으로 바꾼다
        addButton(R.string.title_name_input) { Intent(this, NameInputActivity::class.java) }
        addButton(R.string.title_diagnosis_intro) { Intent(this, DiagnosisIntroActivity::class.java) }
        addButton(R.string.title_diagnosis_question) { Intent(this, DiagnosisQuestionActivity::class.java) }
        addButton(R.string.title_diagnosis_result) { Intent(this, DiagnosisResultActivity::class.java) }
        addButton(R.string.game_title_select) { Intent(this, GameSelectActivity::class.java) }
        addButton(R.string.title_record) { Intent(this, RecordActivity::class.java) }
        addButton(R.string.title_guardian_role) { Intent(this, GuardianRoleSelectActivity::class.java) }
        addButton(R.string.title_guardian_consent) { Intent(this, GuardianConsentActivity::class.java) }
        addButton(R.string.title_guardian_code) { Intent(this, GuardianCodeActivity::class.java) }
        addButton(R.string.title_guardian_enter_code) { Intent(this, GuardianEnterCodeActivity::class.java) }
        addButton(R.string.title_guardian_home) { Intent(this, GuardianHomeActivity::class.java) }
        addButton(R.string.title_settings) { Intent(this, SettingsActivity::class.java) }

        // 디버그 메뉴는 디버그 빌드에만 있어서 이름으로 연다
        if (BuildConfig.DEBUG) {
            addButton(R.string.title_debug) {
                Intent().setClassName(this, "com.team3.dorawing.ui.debug.DebugActivity")
            }
        }
    }

    private fun addButton(textRes: Int, makeIntent: () -> Intent) {
        val button = MaterialButton(this)
        button.setText(textRes)
        button.setOnClickListener { startActivity(makeIntent()) }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.topMargin = resources.getDimensionPixelSize(R.dimen.gap)
        binding.buttonList.addView(button, params)
    }
}

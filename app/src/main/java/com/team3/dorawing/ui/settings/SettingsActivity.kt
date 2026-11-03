package com.team3.dorawing.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivitySettingsBinding
import com.team3.dorawing.ui.guardian.GuardianRoleSelectActivity
import com.team3.dorawing.util.Prefs
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

// 음성 안내 켜기/끄기, 이름 바꾸기, 보호자 연결. 글씨 크기는 휴대폰 설정을 따른다
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val speech = getString(R.string.settings_speech)
        TopBar.setup(this, binding.topBar, getString(R.string.title_settings)) { speech }
        Speaker.speak(this, speech)

        binding.btnVoice.setOnClickListener { toggleVoice() }
        showVoice()

        val name = currentName()
        binding.etName.setText(name)
        binding.tvNameNow.text = getString(R.string.settings_name_now, name)
        binding.btnSaveName.setOnClickListener { saveName() }
        binding.etName.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) saveName()
            true
        }

        // 내 기록의 보호자 연결 버튼과 같은 화면으로
        binding.btnGuardian.setOnClickListener { startActivity(Intent(this, GuardianRoleSelectActivity::class.java)) }
    }

    private fun currentName(): String = Prefs.getName(this) ?: getString(R.string.name_default)

    private fun showVoice() {
        val on = Prefs.isVoiceOn(this)
        binding.tvVoice.setText(if (on) R.string.settings_voice_on else R.string.settings_voice_off)
        binding.btnVoice.setText(if (on) R.string.settings_voice_turn_off else R.string.settings_voice_turn_on)
    }

    // 끄면 읽던 것도 바로 멈춘다. 켜면 켜졌다고 읽어 준다
    private fun toggleVoice() {
        val on = !Prefs.isVoiceOn(this)
        Speaker.setVoiceOn(this, on)
        showVoice()
        if (on) Speaker.speak(this, getString(R.string.settings_voice_done))
    }

    // 비우면 이름 입력 화면의 건너뛰기처럼 "어르신"
    private fun saveName() {
        val name = binding.etName.text.toString().trim().ifEmpty { getString(R.string.name_default) }
        Prefs.setName(this, name)
        binding.etName.setText(name)
        binding.tvNameNow.text = getString(R.string.settings_name_now, name)

        binding.etName.clearFocus()
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(binding.etName.windowToken, 0)

        val done = getString(R.string.settings_name_done, name)
        Toast.makeText(this, done, Toast.LENGTH_SHORT).show()
        Speaker.speak(this, done)
    }
}

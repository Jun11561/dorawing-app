package com.team3.dorawing.ui.guardian

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGuardianRoleSelectBinding
import com.team3.dorawing.util.Prefs
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

// 보호자 연결 입구. 내 기록과 설정 화면에서 연다
class GuardianRoleSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuardianRoleSelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardianRoleSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val speech = getString(R.string.role_speech)
        TopBar.setup(this, binding.topBar, getString(R.string.title_guardian_role)) { speech }
        Speaker.speak(this, speech)

        // 어르신: 동의를 한 번 했으면 바로 코드 화면
        binding.btnElder.setOnClickListener {
            val next = if (Prefs.isConsentDone(this)) GuardianCodeActivity::class.java else GuardianConsentActivity::class.java
            startActivity(Intent(this, next))
            finish()
        }

        // 보호자: 이 폰은 다음부터 보호자 화면으로 열린다
        binding.btnGuardian.setOnClickListener {
            Prefs.setGuardianMode(this, true)
            val home = Intent(this, GuardianHomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (Prefs.getLinkedElder(this) == null) {
                startActivities(arrayOf(home, Intent(this, GuardianEnterCodeActivity::class.java)))
            } else {
                startActivity(home)
            }
        }
    }
}

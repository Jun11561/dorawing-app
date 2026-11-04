package com.team3.dorawing.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.team3.dorawing.BuildConfig
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityHomeBinding
import com.team3.dorawing.db.AppDatabase
import com.team3.dorawing.ui.diagnosis.DiagnosisIntroActivity
import com.team3.dorawing.ui.game.GameSelectActivity
import com.team3.dorawing.ui.guardian.Cloud
import com.team3.dorawing.ui.guardian.GuardianHomeActivity
import com.team3.dorawing.ui.guardian.GuardianRoleSelectActivity
import com.team3.dorawing.ui.record.RecordActivity
import com.team3.dorawing.ui.settings.SettingsActivity
import com.team3.dorawing.util.Dates
import com.team3.dorawing.util.Prefs
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 보호자용으로 정한 폰은 바로 보호자 화면으로
        if (Prefs.isGuardianMode(this)) {
            startActivity(Intent(this, GuardianHomeActivity::class.java))
            finish()
            return
        }

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TopBar.setup(this, binding.topBar, "", showHome = false) { greetingSpeech() }
        // 홈은 인사말이 앱의 얼굴: 띠 안에서 제목 크기로, 아래에 앱 그림과 부제
        binding.topBar.boxSubtitle.visibility = View.VISIBLE

        // 홈 화면에서는 홈 버튼 자리에 설정 버튼을 둔다
        binding.topBar.btnHome.apply {
            visibility = View.VISIBLE
            setText(R.string.home_settings)
            setIconResource(R.drawable.ic_settings)
            setOnClickListener { startActivity(Intent(this@HomeActivity, SettingsActivity::class.java)) }
        }

        binding.btnDiagnosis.setOnClickListener { startActivity(Intent(this, DiagnosisIntroActivity::class.java)) }
        binding.btnGame.setOnClickListener { startActivity(Intent(this, GameSelectActivity::class.java)) }
        binding.btnRecord.setOnClickListener { startActivity(Intent(this, RecordActivity::class.java)) }
        binding.cardHealth.setOnClickListener { startActivity(Intent(this, HealthCardActivity::class.java)) }

        // 디버그 메뉴는 디버그 빌드에만 있어서 클래스 이름으로 연다
        if (BuildConfig.DEBUG) {
            binding.btnDebug.visibility = View.VISIBLE
            binding.btnDebug.setOnClickListener {
                startActivity(Intent().setClassName(this, "com.team3.dorawing.ui.debug.DebugActivity"))
            }
        }

        // 처음 설치: 누가 쓰는지 먼저 묻고, 어르신용이면 그다음에 이름 입력
        if (Prefs.getName(this) == null) {
            if (Prefs.isRoleChosen(this)) {
                startActivity(Intent(this, NameInputActivity::class.java))
            } else {
                startActivity(GuardianRoleSelectActivity.firstRunIntent(this))
            }
        }

        // 앱을 열 때 못 올린 요약을 다시 올리고, 보호자가 모두 떠났으면 서버 요약을 지운다
        Cloud.syncLater(this)
    }

    // 다른 화면에서 돌아올 때마다 이름, 도장을 새로 보여 주고 인사한다
    override fun onResume() {
        super.onResume()
        if (Prefs.getName(this) == null) return
        binding.topBar.tvTitle.text = getString(R.string.home_greeting, Prefs.callName(this))
        Speaker.speak(this, greetingSpeech())

        val today = Dates.dayText(LocalDate.now())
        val cardsDone = Prefs.getCardsDoneDay(this) == today
        binding.tvCardsStatus.setText(if (cardsDone) R.string.home_cards_done else R.string.home_cards_todo)
        showStamp(binding.stampCards, cardsDone)

        lifecycleScope.launch {
            val sets = AppDatabase.get(this@HomeActivity).attendanceDao().get(today)?.completedSets ?: 0
            binding.tvAttend.text = if (sets > 0) getString(R.string.home_attend_done, sets)
            else getString(R.string.home_attend_todo)
            showStamp(binding.stampAttend, sets > 0)
        }
    }

    private fun greetingSpeech(): String =
        getString(R.string.home_greeting_speech, Prefs.callName(this))

    // 받은 도장은 진한 코랄에 흰 체크, 아직이면 회색 점선
    private fun showStamp(stamp: TextView, done: Boolean) {
        stamp.setBackgroundResource(if (done) R.drawable.bg_stamp else R.drawable.bg_stamp_empty)
        stamp.setTextColor(ContextCompat.getColor(this, if (done) R.color.white else R.color.text_sub))
        val check = if (done) ContextCompat.getDrawable(this, R.drawable.ic_check) else null
        stamp.setCompoundDrawablesRelativeWithIntrinsicBounds(null, check, null, null)
    }
}

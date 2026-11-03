package com.team3.dorawing.ui.guardian

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGuardianRoleSelectBinding
import com.team3.dorawing.ui.home.NameInputActivity
import com.team3.dorawing.util.Prefs
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

// 누가 쓰시나요. 처음 설치했을 때(firstRun)와 설정의 보호자 연결에서 연다
class GuardianRoleSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuardianRoleSelectBinding

    // 허용하든 거절하든 보호자 화면으로 넘어간다. 거절하면 휴대폰 알림만 안 뜬다
    private val askNotify = registerForActivityResult(ActivityResultContracts.RequestPermission()) { openGuardian() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardianRoleSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val firstRun = intent.getBooleanExtra("firstRun", false)
        val speech = getString(R.string.role_speech)
        // 처음 설치 때는 아직 홈이 없어서 홈 버튼을 숨기고, 뒤로 가기는 앱을 닫는다
        val title = getString(if (firstRun) R.string.title_first_run else R.string.title_guardian_role)
        TopBar.setup(this, binding.topBar, title, showHome = !firstRun) { speech }
        Speaker.speak(this, speech)
        if (firstRun) onBackPressedDispatcher.addCallback(this) { finishAffinity() }

        // 어르신: 처음 설치면 이름 입력으로. 보호자 연결로 왔으면 동의(한 번 했으면 바로 연결 번호)
        binding.btnElder.setOnClickListener {
            Prefs.setRoleChosen(this)
            val next = when {
                firstRun -> NameInputActivity::class.java
                Prefs.isConsentDone(this) -> GuardianCodeActivity::class.java
                else -> GuardianConsentActivity::class.java
            }
            startActivity(Intent(this, next))
            finish()
        }

        // 보호자: 이 폰은 다음부터 보호자 화면으로 열린다
        binding.btnGuardian.setOnClickListener {
            Prefs.setRoleChosen(this)
            Prefs.setGuardianMode(this, true)
            // Android 13부터는 휴대폰 알림을 띄우려면 허락을 받아야 한다. 시스템 창 전에 이유를 먼저 알려 준다
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                explainNotify()
            } else {
                openGuardian()
            }
        }
    }

    // Android 13 이상에서만 부른다
    @RequiresApi(33)
    private fun explainNotify() {
        val message = getString(R.string.role_notify_message)
        Speaker.speak(this, message)
        // 버튼 하나(다음)만 두고, 누르면 시스템 알림 권한 창
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.role_notify_title)
            .setMessage(message)
            .setNegativeButton(R.string.role_notify_ok) { _, _ -> askNotify.launch(Manifest.permission.POST_NOTIFICATIONS) }
            .setCancelable(false)
            .show()
    }

    private fun openGuardian() {
        val home = Intent(this, GuardianHomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (Prefs.getLinkedElder(this) == null) {
            startActivities(arrayOf(home, Intent(this, GuardianEnterCodeActivity::class.java)))
        } else {
            startActivity(home)
        }
    }

    companion object {
        fun firstRunIntent(context: Context): Intent =
            Intent(context, GuardianRoleSelectActivity::class.java).putExtra("firstRun", true)
    }
}

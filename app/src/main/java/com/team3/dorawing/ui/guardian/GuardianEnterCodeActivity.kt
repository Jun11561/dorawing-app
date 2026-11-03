package com.team3.dorawing.ui.guardian

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGuardianEnterCodeBinding
import com.team3.dorawing.util.Prefs
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 보호자: 어르신 폰의 코드와 내 호칭을 넣어 연결 요청을 보내고 승인을 기다린다
class GuardianEnterCodeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuardianEnterCodeBinding
    private var waitJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardianEnterCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val guide = getString(R.string.enter_guide)
        // 보호자 폰의 홈은 보호자 화면
        TopBar.setup(this, binding.topBar, getString(R.string.title_guardian_enter_code), onHome = { openGuardianHome() }) { guide }
        Speaker.speak(this, guide)

        binding.btnSend.setOnClickListener { send() }
        binding.btnCancel.setOnClickListener { cancelWaiting() }

        if (!Cloud.isReady(this)) {
            showMessage(getString(R.string.guardian_no_firebase))
            binding.btnSend.isEnabled = false
            return
        }
        // 승인을 기다리다가 화면을 닫았으면 이어서 기다린다
        Prefs.getPendingElder(this)?.let { startWaiting(it) }
    }

    private fun lock() = PairRules.Lock(Prefs.getCodeFails(this), Prefs.getCodeLockedUntil(this))

    private fun send() {
        val now = System.currentTimeMillis()
        val lock = lock()
        if (PairRules.isLocked(lock, now)) {
            showMessage(getString(R.string.enter_locked, PairRules.minutesLeft(lock, now)))
            return
        }
        val code = binding.etCode.text.toString().trim()
        val name = binding.etName.text.toString().trim()
        // 자리 수가 모자란 건 틀린 횟수에 넣지 않는다
        if (!PairRules.isValidFormat(code)) {
            showMessage(getString(R.string.enter_need_code))
            return
        }
        if (name.isEmpty()) {
            showMessage(getString(R.string.enter_need_name))
            return
        }

        binding.btnSend.isEnabled = false
        showMessage(getString(R.string.guardian_loading))
        lifecycleScope.launch {
            try {
                val me = Cloud.signIn()
                val (result, elder) = Cloud.checkCode(code)
                if (result == Cloud.CodeCheck.OK && elder != null && Cloud.sendRequest(elder, me, code, name)) {
                    Prefs.setCodeLock(this@GuardianEnterCodeActivity, 0, 0)
                    val callName = binding.etCallName.text.toString().trim().ifEmpty { getString(R.string.name_default) }
                    Prefs.setElderCallName(this@GuardianEnterCodeActivity, callName)
                    Prefs.setPendingElder(this@GuardianEnterCodeActivity, elder)
                    startWaiting(elder)
                } else {
                    fail(if (result == Cloud.CodeCheck.WRONG) R.string.enter_wrong else R.string.enter_expired)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                showMessage(getString(R.string.guardian_offline))
            }
            binding.btnSend.isEnabled = true
        }
    }

    // 틀리면 남은 횟수를 알려 주고, 5번째면 10분 동안 막는다
    private fun fail(message: Int) {
        val now = System.currentTimeMillis()
        val next = PairRules.afterFail(lock(), now)
        Prefs.setCodeLock(this, next.fails, next.lockedUntil)
        if (PairRules.isLocked(next, now)) {
            showMessage(getString(R.string.enter_locked, PairRules.minutesLeft(next, now)))
        } else {
            showMessage(getString(message, PairRules.triesLeft(next)))
        }
    }

    private fun showMessage(text: String) {
        binding.tvMessage.visibility = View.VISIBLE
        binding.tvMessage.text = text
        Speaker.speak(this, text)
    }

    // 어르신이 승인하면 어르신 문서를 읽을 수 있게 된다. 3초마다 확인
    private fun startWaiting(elder: String) {
        binding.boxForm.visibility = View.GONE
        binding.boxWaiting.visibility = View.VISIBLE
        Speaker.speak(this, getString(R.string.enter_waiting))
        waitJob?.cancel()
        waitJob = lifecycleScope.launch {
            while (true) {
                val approved = try {
                    Cloud.isApproved(elder, Cloud.signIn())
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    false
                }
                if (approved) {
                    Prefs.setLinkedElder(this@GuardianEnterCodeActivity, elder)
                    Prefs.setPendingElder(this@GuardianEnterCodeActivity, null)
                    openGuardianHome()
                    return@launch
                }
                delay(3000)
            }
        }
    }

    private fun cancelWaiting() {
        waitJob?.cancel()
        Prefs.setPendingElder(this, null)
        binding.boxWaiting.visibility = View.GONE
        binding.boxForm.visibility = View.VISIBLE
        binding.tvMessage.visibility = View.GONE
    }

    private fun openGuardianHome() {
        startActivity(
            Intent(this, GuardianHomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }
}

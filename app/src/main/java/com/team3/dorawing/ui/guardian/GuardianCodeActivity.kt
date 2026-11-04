package com.team3.dorawing.ui.guardian

import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.ContextThemeWrapper
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGuardianCodeBinding
import com.team3.dorawing.util.Prefs
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// 어르신: 6자리 연결 번호를 보여 주고, 보호자 요청이 오면 최종 승인한다. 연결된 보호자를 끊을 수도 있다
class GuardianCodeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuardianCodeBinding
    private var uid: String? = null
    private var guardians = listOf<String>()
    private var expiresAt = 0L
    private var asking = false
    private var speech = ""
    private val listeners = mutableListOf<ListenerRegistration>()
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (showTimer()) handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardianCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        speech = getString(R.string.title_guardian_code)
        TopBar.setup(this, binding.topBar, getString(R.string.title_guardian_code)) { speech }

        binding.btnRetry.setOnClickListener { start() }
        binding.btnNewCode.setOnClickListener { lifecycleScope.launch { makeCode() } }

        if (!Cloud.isReady(this)) {
            showStatus(getString(R.string.guardian_no_firebase), retry = false)
            return
        }
        start()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tick)
        listeners.forEach { it.remove() }
    }

    private fun start() {
        showStatus(getString(R.string.guardian_loading), retry = false)
        lifecycleScope.launch {
            try {
                val me = Cloud.signIn()
                checkReinstall(me)
                guardians = Cloud.elderGuardians(me)
                uid = me
                if (guardians.isNotEmpty()) Prefs.setHasGuardian(this@GuardianCodeActivity, true)
                showStatus(null, retry = false)
                showGuardians()
                listen(me)
                prepareCode()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                showStatus(getString(R.string.guardian_offline), retry = true)
            }
        }
    }

    // 다시 설치해서 익명 uid가 바뀌면 예전 연결은 쓸 수 없다
    private fun checkReinstall(me: String) {
        val before = Prefs.getElderUid(this)
        if (before != null && before != me) {
            binding.cardReinstall.visibility = View.VISIBLE
            Prefs.setHasGuardian(this, false)
            Prefs.clearCode(this)
        }
        Prefs.setElderUid(this, me)
    }

    private fun listen(me: String) {
        if (listeners.isNotEmpty()) return
        listeners += Cloud.listenElder(me) { list ->
            guardians = list
            showGuardians()
            if (!PairRules.canAddGuardian(list.size)) {
                showFull()
            } else if (binding.cardCode.visibility != View.VISIBLE) {
                // 보호자가 줄어서 자리가 생기면 새 번호를 받을 수 있게
                binding.tvFull.visibility = View.GONE
                binding.cardCode.visibility = View.VISIBLE
                showCodeUsed()
            }
        }
        listeners += Cloud.listenRequests(me) { requests -> requests.firstOrNull()?.let { ask(it) } }
    }

    // 보던 코드가 아직 쓸 수 있으면 그대로, 아니면 새로
    private suspend fun prepareCode() {
        if (!PairRules.canAddGuardian(guardians.size)) {
            showFull()
            return
        }
        val saved = Prefs.getCode(this)
        val savedExpires = Prefs.getCodeExpires(this)
        if (saved != null && PairRules.isUsable(savedExpires, false, System.currentTimeMillis())) {
            showCode(saved, savedExpires)
        } else {
            makeCode()
        }
    }

    private suspend fun makeCode() {
        val me = uid ?: return
        binding.btnNewCode.isEnabled = false
        try {
            Prefs.getCode(this)?.let { Cloud.deleteCode(it) }
            val (code, expires) = Cloud.newCode(me)
            Prefs.setCode(this, code, expires)
            showCode(code, expires)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            showStatus(getString(R.string.guardian_offline), retry = true)
        }
        binding.btnNewCode.isEnabled = true
    }

    private fun showCode(code: String, expires: Long) {
        expiresAt = expires
        binding.tvFull.visibility = View.GONE
        binding.cardCode.visibility = View.VISIBLE
        binding.tvCode.visibility = View.VISIBLE
        // 읽기 쉽게 세 자리씩 띄운다
        binding.tvCode.text = getString(R.string.code_split, code.take(3), code.drop(3))
        binding.btnNewCode.visibility = View.GONE
        handler.removeCallbacks(tick)
        handler.post(tick)
        // 번호는 한 자리씩 읽어 준다
        speech = getString(R.string.code_speech, code.toList().joinToString(", "))
        Speaker.speak(this, speech)
    }

    // 남은 시간. 끝났으면 false
    private fun showTimer(): Boolean {
        val left = expiresAt - System.currentTimeMillis()
        if (left <= 0) {
            binding.tvCode.alpha = 0.3f
            binding.tvTimer.setText(R.string.code_expired)
            binding.btnNewCode.visibility = View.VISIBLE
            return false
        }
        binding.tvCode.alpha = 1f
        val seconds = (left + 999) / 1000
        binding.tvTimer.text = getString(R.string.code_timer, (seconds / 60).toInt(), (seconds % 60).toInt())
        return true
    }

    // 코드를 썼을 때: 다른 보호자를 더 연결하려면 새 번호를 받는다
    private fun showCodeUsed() {
        handler.removeCallbacks(tick)
        binding.tvCode.visibility = View.GONE
        binding.tvTimer.setText(R.string.code_used)
        binding.btnNewCode.visibility = View.VISIBLE
    }

    private fun showFull() {
        handler.removeCallbacks(tick)
        binding.cardCode.visibility = View.GONE
        binding.tvFull.visibility = View.VISIBLE
    }

    private fun showStatus(text: String?, retry: Boolean) {
        binding.tvStatus.visibility = if (text == null) View.GONE else View.VISIBLE
        binding.tvStatus.text = text
        binding.btnRetry.visibility = if (retry) View.VISIBLE else View.GONE
        if (text != null) {
            speech = text
            Speaker.speak(this, text)
        }
    }

    private fun nameOf(guardianUid: String) =
        Prefs.getGuardianName(this, guardianUid) ?: getString(R.string.code_guardian_unknown)

    // 보호자마다 이름 아래에 연결 끊기 버튼
    private fun showGuardians() {
        binding.cardGuardians.visibility = View.VISIBLE
        binding.tvNoGuardian.visibility = if (guardians.isEmpty()) View.VISIBLE else View.GONE
        binding.guardianList.removeAllViews()
        val gap = resources.getDimensionPixelSize(R.dimen.gap)
        for (guardianUid in guardians) {
            val name = nameOf(guardianUid)
            val label = TextView(this).apply {
                text = getString(R.string.code_guardian_row, name)
                setPadding(0, gap, 0, 0)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_heading))
                setTypeface(typeface, Typeface.BOLD)
            }
            // 연결 끊기는 어느 화면이든 테두리 버튼
            val button = MaterialButton(ContextThemeWrapper(this, R.style.ThemeOverlay_Dorawing_SecondaryButton)).apply {
                setText(R.string.code_disconnect)
                setOnClickListener { confirmDisconnect(guardianUid, name) }
            }
            binding.guardianList.addView(label)
            binding.guardianList.addView(
                button,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = gap / 2 }
            )
        }
    }

    private fun ask(request: Cloud.Request) {
        if (asking || isFinishing) return
        val me = uid ?: return
        // 이미 2명이면 받을 수 없어서 요청만 지운다
        if (!PairRules.canAddGuardian(guardians.size)) {
            lifecycleScope.launch { runCatching { Cloud.reject(me, request) } }
            return
        }
        asking = true
        val name = request.name.ifBlank { getString(R.string.code_guardian_unknown) }
        val title = getString(R.string.code_request_title, name)
        Speaker.speak(this, "$title ${getString(R.string.code_request_message)}")
        // 꽉 찬 버튼이 연결하기
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(R.string.code_request_message)
            .setNegativeButton(R.string.code_request_yes) { _, _ -> approve(me, request, name) }
            .setPositiveButton(R.string.code_request_no) { _, _ -> lifecycleScope.launch { runCatching { Cloud.reject(me, request) } } }
            .setCancelable(false)
            .setOnDismissListener { asking = false }
            .show()
    }

    private fun approve(me: String, request: Cloud.Request, name: String) {
        lifecycleScope.launch {
            try {
                val current = request.code == Prefs.getCode(this@GuardianCodeActivity)
                // 서버가 바뀌면 목록이 바로 다시 그려지므로 호칭을 먼저 적어 둔다
                Prefs.setGuardianName(this@GuardianCodeActivity, request.guardianUid, name)
                Cloud.approve(me, request, markCodeUsed = current)
                Prefs.setHasGuardian(this@GuardianCodeActivity, true)
                if (current) {
                    Prefs.clearCode(this@GuardianCodeActivity)
                    showCodeUsed()
                }
                val done = getString(R.string.code_connected, name)
                Toast.makeText(this@GuardianCodeActivity, done, Toast.LENGTH_LONG).show()
                Speaker.speak(this@GuardianCodeActivity, done)
                // 보호자가 바로 볼 수 있게 지금 기록을 올린다
                Cloud.uploadLater(this@GuardianCodeActivity)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Prefs.removeGuardianName(this@GuardianCodeActivity, request.guardianUid)
                Toast.makeText(this@GuardianCodeActivity, R.string.guardian_offline, Toast.LENGTH_LONG).show()
            }
            showGuardians()
        }
    }

    // 안전한 쪽(그대로 두기)을 꽉 찬 버튼으로
    private fun confirmDisconnect(guardianUid: String, name: String) {
        val me = uid ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.code_disconnect_title, name))
            .setMessage(R.string.code_disconnect_message)
            .setPositiveButton(R.string.code_disconnect) { _, _ ->
                lifecycleScope.launch {
                    try {
                        Cloud.removeGuardian(this@GuardianCodeActivity, me, guardianUid)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Toast.makeText(this@GuardianCodeActivity, R.string.guardian_offline, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(R.string.code_keep, null)
            .show()
    }
}

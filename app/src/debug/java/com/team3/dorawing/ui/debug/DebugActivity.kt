package com.team3.dorawing.ui.debug

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityDebugBinding
import com.team3.dorawing.db.AppDatabase
import com.team3.dorawing.game.GameRecorder
import com.team3.dorawing.game.GameStats
import com.team3.dorawing.game.GameType
import com.team3.dorawing.ui.game.GameLauncher
import com.team3.dorawing.ui.guardian.Cloud
import com.team3.dorawing.ui.guardian.GuardianAlerts
import com.team3.dorawing.ui.guardian.GuardianHomeActivity
import com.team3.dorawing.ui.guardian.Summary
import com.team3.dorawing.util.Dates
import com.team3.dorawing.util.Prefs
import com.team3.dorawing.util.TopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

// 디버그 빌드에만 들어가는 화면. 가상 기록을 넣고 게임 화면을 바로 띄운다
class DebugActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDebugBinding
    private val buttons = mutableListOf<MaterialButton>()

    // 보호자 연결 확인 결과. 상태 글 아래에 붙인다
    private var note = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)
        TopBar.setup(this, binding.topBar, getString(R.string.title_debug))

        addButton(binding.scenarioList, "1. 5주 꾸준히 보통") { steady() }
        addButton(binding.scenarioList, "2. 최근 1주 ${GameType.entries.first().label} 하락") { oneDrop() }
        addButton(binding.scenarioList, "3. 오늘 2세트") { todayTwo() }
        addButton(binding.scenarioList, "전부 지우기") { clearAll() }

        for (type in GameType.entries) {
            addButton(binding.gameList, "${type.label} Lv.3 바로 열기", runInBackground = false) {
                startActivity(GameLauncher.intentFor(this, type, 3))
            }
        }

        // 보호자 연결: 가상 보호자는 서버 규칙을 그대로 거친다
        addButton(binding.guardianList, "가상 보호자 연결 요청") { note = guardianStep { FakeGuardian.request(this) } }
        addButton(binding.guardianList, "가상 보호자로 올라온 기록 읽기") { note = guardianStep { FakeGuardian.read(this) } }
        addButton(binding.guardianList, "가상 보호자 쪽에서 연결 끊기") { note = guardianStep { FakeGuardian.leave(this) } }
        addButton(binding.guardianList, "가상 보호자로 알림 확인 지금 실행") { note = guardianStep { checkAlerts() } }
        addButton(binding.guardianList, "보호자 화면 미리 보기 (내 기록)", runInBackground = false) {
            val me = Prefs.getElderUid(this)
            if (me == null) {
                binding.tvStatus.text = "어르신 uid가 없어요. 먼저 연결 번호 화면을 열어 주세요"
            } else {
                Prefs.setLinkedElder(this, me)
                startActivity(Intent(this, GuardianHomeActivity::class.java))
            }
        }

        lifecycleScope.launch { showStatus() }
    }

    // 보호자 폰의 1시간 작업과 같은 확인(GuardianAlerts.check)을 가상 보호자가 읽은 요약으로 지금 한다
    private suspend fun checkAlerts(): String {
        val data = FakeGuardian.summary(this) ?: return "가상 보호자: 읽을 요약이 없어요. 연결하고 기록을 올린 뒤 해 주세요"
        // 알림을 누르면 이 폰의 기록이 보호자 화면으로 보이게 (보호자 화면 미리 보기와 같음)
        Prefs.getElderUid(this)?.let { Prefs.setLinkedElder(this, it) }
        val inSummary = Summary.fromMap(data).alerts.size
        val shown = GuardianAlerts.check(this, data)
        val denied = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        return "요약의 변화 알림 ${inSummary}개, 새로 띄운 휴대폰 알림 ${shown}번" +
            if (denied) "\n알림 권한이 없어서 띄우지 못했어요 (EMULATOR.md의 pm grant)" else ""
    }

    // 1, 2번은 게임 기록만 지우고 넣는다. 자가진단 결과는 남긴다
    private suspend fun steady() {
        clearGames()
        DebugScenarios.steady().forEach { record(it) }
    }

    private suspend fun oneDrop() {
        clearGames()
        DebugScenarios.oneDrop().forEach { record(it) }
    }

    private suspend fun todayTwo() {
        DebugScenarios.todayTwo().forEach { record(it) }
    }

    private suspend fun clearGames() {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.get(this@DebugActivity)
            db.gameSetDao().deleteAll()
            db.attendanceDao().deleteAll()
            db.levelStateDao().deleteAll()
            db.changeAlertDao().deleteAll()
        }
    }

    private suspend fun clearAll() {
        withContext(Dispatchers.IO) { AppDatabase.get(this@DebugActivity).clearAllTables() }
    }

    private suspend fun record(set: PlannedSet) {
        // 오늘 기록은 지금 시각, 과거 기록은 그날 오전 10시부터 영역마다 10분씩
        val playedAt = if (set.daysAgo == 0) {
            System.currentTimeMillis()
        } else {
            Dates.toMillis(LocalDate.now().minusDays(set.daysAgo.toLong())) + (10 * 60 + set.order * 10) * 60_000L
        }
        val level = GameStats.currentLevel(this, set.type)
        GameRecorder.finish(this, set.type, level, set.correct, DebugScenarios.TOTAL, 1500, playedAt)
    }

    private suspend fun showStatus() {
        val db = AppDatabase.get(this)
        val lines = mutableListOf("세트 기록 ${db.gameSetDao().count()}개")
        for (type in GameType.entries) {
            val state = db.levelStateDao().get(type.name)
            lines += if (state == null) "${type.label}: 기록 없음"
            else "${type.label}: Lv.${state.level}, 전체 ${state.totalSets}세트, 레벨 변경 뒤 ${state.setsSinceLevelChange}세트"
        }
        val alerts = db.changeAlertDao().active()
        val names = alerts.mapNotNull { GameType.fromName(it.gameType)?.label }
        lines += "변화 알림 ${alerts.size}개 ${if (names.isEmpty()) "" else names.joinToString(prefix = "(", postfix = ")")}"
        if (note.isNotEmpty()) lines += "\n$note"
        binding.tvStatus.text = lines.joinToString("\n")
    }

    private suspend fun guardianStep(step: suspend () -> String): String {
        if (!Cloud.isReady(this)) return "google-services.json이 없어서 Firebase를 쓸 수 없어요"
        return try {
            step()
        } catch (e: Exception) {
            "실패: ${e.message}"
        }
    }

    private fun addButton(list: LinearLayout, text: String, runInBackground: Boolean = true, action: suspend () -> Unit) {
        val button = MaterialButton(this)
        button.text = text
        button.setOnClickListener {
            if (!runInBackground) {
                lifecycleScope.launch { action() }
                return@setOnClickListener
            }
            // 넣는 동안 다른 버튼을 못 누르게
            buttons.forEach { it.isEnabled = false }
            binding.tvStatus.text = "넣는 중..."
            lifecycleScope.launch {
                action()
                showStatus()
                buttons.forEach { it.isEnabled = true }
            }
        }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.topMargin = resources.getDimensionPixelSize(R.dimen.gap)
        list.addView(button, params)
        buttons += button
    }
}

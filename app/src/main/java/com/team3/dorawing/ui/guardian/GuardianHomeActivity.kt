package com.team3.dorawing.ui.guardian

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGuardianHomeBinding
import com.team3.dorawing.game.GameStats
import com.team3.dorawing.game.GameType
import com.team3.dorawing.util.Dates
import com.team3.dorawing.util.Prefs
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

// 보호자 폰의 첫 화면. 연결된 어르신이 올린 요약만 보여 준다
class GuardianHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuardianHomeBinding
    private var speech = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardianHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 이 화면이 보호자 폰의 홈이라 홈 버튼은 숨긴다. 제목은 "어머님의 기록"처럼 보호자가 정한 호칭으로
        TopBar.setup(this, binding.topBar, title(), showHome = false) { speech }

        binding.btnEnterCode.setOnClickListener { startActivity(Intent(this, GuardianEnterCodeActivity::class.java)) }
        binding.btnRefresh.setOnClickListener { load() }
        binding.btnLeave.setOnClickListener { confirmLeave() }
        binding.btnElderMode.setOnClickListener {
            Prefs.setGuardianMode(this, false)
            // 어르신 모드 폰에서는 알림 확인 작업이 돌지 않게
            GuardianAlerts.cancel(this)
            TopBar.goHome(this)
        }
        setupChart()

        // 보호자 폰에서만 1시간마다 새 변화 알림 확인 (디버그 미리 보기로 연 어르신 폰에서는 안 함)
        if (Prefs.isGuardianMode(this)) GuardianAlerts.schedule(this)
    }

    override fun onResume() {
        super.onResume()
        // 연결 번호 화면에서 호칭을 넣고 돌아오면 바뀐다
        binding.topBar.tvTitle.text = title()
        load()
    }

    private fun title() = getString(R.string.ghome_title, Prefs.getElderCallName(this))

    private fun load() {
        if (!Cloud.isReady(this)) {
            showStatus(getString(R.string.guardian_no_firebase))
            return
        }
        val elder = Prefs.getLinkedElder(this)
        if (elder == null) {
            showNotLinked(getString(R.string.ghome_not_linked))
            return
        }
        binding.btnRefresh.visibility = View.VISIBLE
        binding.btnLeave.visibility = View.VISIBLE
        binding.btnEnterCode.visibility = View.GONE
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.setText(R.string.guardian_loading)
        lifecycleScope.launch {
            try {
                val data = Cloud.loadSummary(elder, Cloud.signIn())
                when {
                    data == null -> {
                        // 어르신이 끊었거나 다시 설치했다
                        Prefs.setLinkedElder(this@GuardianHomeActivity, null)
                        showNotLinked(getString(R.string.ghome_disconnected))
                    }
                    data.isEmpty() -> showStatus(getString(R.string.ghome_no_data))
                    else -> show(Summary.fromMap(data))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                showStatus(getString(R.string.guardian_offline))
            }
        }
    }

    private fun showNotLinked(text: String) {
        binding.boxData.visibility = View.GONE
        binding.btnRefresh.visibility = View.GONE
        binding.btnLeave.visibility = View.GONE
        binding.btnEnterCode.visibility = View.VISIBLE
        showStatus(text)
    }

    private fun showStatus(text: String) {
        binding.boxData.visibility = View.GONE
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.text = text
        speech = text
        Speaker.speak(this, text)
    }

    private fun show(summary: Summary) {
        binding.tvStatus.visibility = View.GONE
        binding.boxData.visibility = View.VISIBLE
        binding.tvUpdated.text = getString(R.string.ghome_updated, Dates.koreanTime(summary.updatedAt))
        // 맨 위 3줄 요약. 지금 GameType에 없는 영역의 알림은 건너뛴다
        val alertNames = summary.alerts.mapNotNull { GameType.fromName(it.area)?.label }.distinct()
        val hasAlert = alertNames.isNotEmpty()
        binding.tvSumAlert.text = if (hasAlert) getString(R.string.ghome_alert_some, alertNames.joinToString(", "))
        else getString(R.string.ghome_alert_none)
        binding.tvAlertNote.visibility = if (hasAlert) View.VISIBLE else View.GONE
        // 알림이 있으면 분홍 바탕과 코랄 테두리, 없으면 흰 바탕과 진한 초록 테두리
        binding.cardSummary.setCardBackgroundColor(ContextCompat.getColor(this, if (hasAlert) R.color.pink_light else R.color.white))
        binding.cardSummary.strokeColor = ContextCompat.getColor(this, if (hasAlert) R.color.coral_dark else R.color.green_dark)
        binding.ivSumAlert.imageTintList = ContextCompat.getColorStateList(this, if (hasAlert) R.color.coral_dark else R.color.green_dark)

        showChart(summary)

        // 지난 14일 출석 중 이번 주만
        val monday = Dates.monday(LocalDate.now())
        val week = (0L..6L).map { monday.plusDays(it) }.filter { Dates.dayText(it) in summary.attendDays }
        val dayNames = week.joinToString(", ") { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN) }
        val weekText = if (week.isEmpty()) getString(R.string.ghome_week, 0)
        else getString(R.string.ghome_week_days, week.size, dayNames)
        binding.tvSumWeek.text = weekText
        val lastText = summary.lastPlayedAt?.let { getString(R.string.ghome_last_play, Dates.koreanTime(it)) }
            ?: getString(R.string.ghome_no_play)
        binding.tvSumLast.text = lastText

        val diag = summary.diagnosis
        if (diag == null) {
            binding.tvDiagDate.visibility = View.GONE
            binding.tvDiag.setText(R.string.record_diag_none)
        } else {
            binding.tvDiagDate.visibility = View.VISIBLE
            binding.tvDiagDate.text = Dates.koreanDay(diag.takenAt)
            val judge = getString(if (diag.suspected) R.string.result_suspected else R.string.result_normal)
            binding.tvDiag.text = getString(R.string.result_judge, judge, diag.score, diag.maxScore)
        }

        // 요약 3줄을 읽는다
        speech = listOf(title(), binding.tvSumAlert.text, weekText, lastText).joinToString(". ")
        Speaker.speak(this, speech)
    }

    private fun showLevels(rows: List<String>) {
        binding.levelList.removeAllViews()
        val gap = resources.getDimensionPixelSize(R.dimen.gap)
        val half = gap / 2
        for (pair in rows.chunked(2)) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEachIndexed { i, text ->
                val chip = TextView(this).apply {
                    this.text = text
                    gravity = Gravity.CENTER
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, R.color.green_dark))
                    setBackgroundResource(R.drawable.bg_chip)
                    setPadding(gap, half, gap, half)
                }
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                if (i == 1) params.marginStart = gap
                line.addView(chip, params)
            }
            // 칸이 하나뿐이면 빈 칸으로 너비를 맞춘다
            if (pair.size == 1) line.addView(Space(this), LinearLayout.LayoutParams(0, 0, 1f).apply { marginStart = gap })
            binding.levelList.addView(line, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = gap })
        }
    }

    private fun setupChart() {
        binding.chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setFitBars(true)
            extraBottomOffset = 8f
            minOffset = 4f
            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 25f
                setDrawLabels(false)
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(context, R.color.text_main)
            }
        }
    }

    // 내 기록 그래프와 같은 모양. 단계는 그래프 아래에 두 개씩 칩으로
    private fun showChart(summary: Summary) {
        val types = GameType.entries
        val entries = types.mapIndexed { i, type ->
            val rate = summary.areas[type.name]?.rate
            BarEntry(i.toFloat(), ((rate ?: 0.0) * 100).toFloat(), rate)
        }
        // 내 기록과 같은 규칙: 다른 영역보다 10%p 이상 낮은 영역만 코랄색
        val lowest = GameStats.lowestArea(types.mapNotNull { type -> summary.areas[type.name]?.rate?.let { type to it } })
        val dataSet = BarDataSet(entries, "").apply {
            colors = types.map { ContextCompat.getColor(this@GuardianHomeActivity, if (it == lowest) R.color.coral else R.color.green_mid) }
            valueTextSize = chartText(listOf("100%"), types.size)
            valueTextColor = ContextCompat.getColor(this@GuardianHomeActivity, R.color.text_main)
            valueFormatter = object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry): String =
                    if (barEntry.data == null) "" else "${barEntry.y.roundToInt()}%"
            }
        }
        binding.tvLowest.visibility = if (lowest == null) View.GONE else View.VISIBLE
        val labels = types.map { it.label }
        binding.chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.chart.xAxis.labelCount = types.size
        binding.chart.xAxis.textSize = chartText(labels, types.size)
        binding.chart.extraTopOffset = dataSet.valueTextSize / resources.displayMetrics.density
        binding.chart.data = BarData(dataSet).apply { barWidth = 0.6f }
        binding.chart.invalidate()

        showLevels(types.mapNotNull { type ->
            summary.areas[type.name]?.let { getString(R.string.ghome_level_row, type.label, it.level) }
        })
        val empty = types.filter { summary.areas[it.name]?.rate == null }.joinToString(", ") { it.label }
        binding.tvNoSets.visibility = if (empty.isEmpty()) View.GONE else View.VISIBLE
        binding.tvNoSets.text = getString(R.string.record_no_sets, empty)
    }

    // 폰의 글자 크기 배율을 곱하되 막대 한 칸에 들어가는 만큼까지만 (내 기록과 같은 계산)
    private fun chartText(texts: List<String>, count: Int): Float {
        val want = 20f * resources.configuration.fontScale
        val width = binding.chart.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val slot = (width / resources.displayMetrics.density - 8f) / count * 0.85f
        val paint = Paint().apply { textSize = 100f }
        return min(want, slot * 100f / texts.maxOf { paint.measureText(it) })
    }

    // 안전한 쪽(그대로 두기)을 꽉 찬 버튼으로
    private fun confirmLeave() {
        val elder = Prefs.getLinkedElder(this) ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ghome_leave_title)
            .setMessage(R.string.ghome_leave_message)
            .setPositiveButton(R.string.ghome_leave) { _, _ ->
                lifecycleScope.launch {
                    try {
                        Cloud.leave(elder, Cloud.signIn())
                        Prefs.setLinkedElder(this@GuardianHomeActivity, null)
                        load()
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Toast.makeText(this@GuardianHomeActivity, R.string.guardian_offline, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(R.string.code_keep, null)
            .show()
    }
}

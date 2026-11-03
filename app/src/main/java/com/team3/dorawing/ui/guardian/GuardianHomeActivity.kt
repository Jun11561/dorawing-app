package com.team3.dorawing.ui.guardian

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
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

        // 이 화면이 보호자 폰의 홈이라 홈 버튼은 숨긴다
        TopBar.setup(this, binding.topBar, getString(R.string.title_guardian_home), showHome = false) { speech }

        binding.btnEnterCode.setOnClickListener { startActivity(Intent(this, GuardianEnterCodeActivity::class.java)) }
        binding.btnRefresh.setOnClickListener { load() }
        binding.btnLeave.setOnClickListener { confirmLeave() }
        binding.btnElderMode.setOnClickListener {
            Prefs.setGuardianMode(this, false)
            TopBar.goHome(this)
        }
        setupChart()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

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
        val parts = mutableListOf<String>()

        // 변화 알림. 지금 GameType에 없는 영역은 건너뛴다
        val alertNames = summary.alerts.mapNotNull { GameType.fromName(it.area)?.label }.distinct()
        binding.cardAlert.visibility = if (alertNames.isEmpty()) View.GONE else View.VISIBLE
        if (alertNames.isNotEmpty()) {
            val text = getString(R.string.ghome_alert_areas, alertNames.joinToString(", "))
            binding.tvAlertAreas.text = text
            parts += getString(R.string.ghome_alert_title)
            parts += text
        }

        showChart(summary)

        // 지난 14일 출석 중 이번 주만
        val monday = Dates.monday(LocalDate.now())
        val week = (0L..6L).map { monday.plusDays(it) }.filter { Dates.dayText(it) in summary.attendDays }
        val dayNames = week.joinToString(", ") { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN) }
        val weekText = if (week.isEmpty()) getString(R.string.ghome_week, 0)
        else getString(R.string.ghome_week_days, week.size, dayNames)
        binding.tvWeek.text = weekText
        parts += weekText
        binding.tvLastPlay.text = summary.lastPlayedAt?.let { getString(R.string.ghome_last_play, Dates.koreanTime(it)) }
            ?: getString(R.string.ghome_no_play)

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

        speech = parts.joinToString(" ")
        Speaker.speak(this, speech)
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

    // 내 기록 그래프와 같은 모양. 단계는 그래프 아래 글로
    private fun showChart(summary: Summary) {
        val types = GameType.entries
        val entries = types.mapIndexed { i, type ->
            val rate = summary.areas[type.name]?.rate
            BarEntry(i.toFloat(), ((rate ?: 0.0) * 100).toFloat(), rate)
        }
        val dataSet = BarDataSet(entries, "").apply {
            color = ContextCompat.getColor(this@GuardianHomeActivity, R.color.green_mid)
            valueTextSize = chartText(listOf("100%"), types.size)
            valueTextColor = ContextCompat.getColor(this@GuardianHomeActivity, R.color.text_main)
            valueFormatter = object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry): String =
                    if (barEntry.data == null) "" else "${barEntry.y.roundToInt()}%"
            }
        }
        val labels = types.map { it.label }
        binding.chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.chart.xAxis.labelCount = types.size
        binding.chart.xAxis.textSize = chartText(labels, types.size)
        binding.chart.extraTopOffset = dataSet.valueTextSize / resources.displayMetrics.density
        binding.chart.data = BarData(dataSet).apply { barWidth = 0.6f }
        binding.chart.invalidate()

        binding.tvLevels.text = types.mapNotNull { type ->
            summary.areas[type.name]?.let { getString(R.string.ghome_level_row, type.label, it.level) }
        }.joinToString("\n")
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

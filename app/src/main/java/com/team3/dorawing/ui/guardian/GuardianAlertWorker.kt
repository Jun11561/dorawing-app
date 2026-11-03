package com.team3.dorawing.ui.guardian

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.team3.dorawing.R
import com.team3.dorawing.game.GameType
import com.team3.dorawing.util.Prefs
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

// 보호자 폰에서 1시간마다 연결된 어르신의 요약을 읽어 새 변화 알림이 있으면 휴대폰 알림을 띄운다 (서버 없이)
class GuardianAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        // 어르신 모드로 바뀐 폰에서는 스스로 멈춘다
        if (!Prefs.isGuardianMode(context)) {
            GuardianAlerts.cancel(context)
            return Result.success()
        }
        val elder = Prefs.getLinkedElder(context) ?: return Result.success()
        if (!Cloud.isReady(context)) return Result.success()
        return try {
            // 연결이 끊겼으면 null, 요약이 없으면 빈 맵이라 확인할 것이 없다
            val data = Cloud.loadSummary(elder, Cloud.signIn())
            if (!data.isNullOrEmpty()) GuardianAlerts.check(context, data)
            Result.success()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.retry()
        }
    }
}

object GuardianAlerts {
    private const val WORK = "guardianAlerts"
    private const val CHANNEL = "guardianAlert"
    private const val NOTIFY_ID = 1

    // 보호자용을 고른 폰에서만 돈다. 이미 있으면 그대로 둔다
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<GuardianAlertWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }

    // 알림 하나를 가리키는 이름. 같은 영역 알림은 7일 안에 다시 생기지 않아서 영역과 날짜로 충분하다
    fun keyOf(alert: Summary.Alert) = "${alert.area}|${alert.day}"

    // 아직 띄우지 않은 알림. 지금 GameType에 없는 영역은 뺀다
    fun newAlerts(alerts: List<Summary.Alert>, shown: Set<String>): List<Summary.Alert> =
        alerts.filter { GameType.fromName(it.area) != null && keyOf(it) !in shown }

    // 기억해 둘 알림: 지금 요약에 있는 것만. 7일이 지나 요약에서 빠진 알림은 다시 오지 않으니 지운다
    fun shownAfter(alerts: List<Summary.Alert>, shown: Set<String>, notified: List<Summary.Alert>): Set<String> {
        val now = alerts.map { keyOf(it) }.toSet()
        return (shown + notified.map { keyOf(it) }).filter { it in now }.toSet()
    }

    // 요약을 보고 새 알림이 있으면 휴대폰 알림을 띄운다. 띄운 알림 수를 돌려준다
    fun check(context: Context, summary: Map<String, Any?>): Int {
        val alerts = Summary.fromMap(summary).alerts
        val shown = Prefs.getShownAlerts(context)
        val fresh = newAlerts(alerts, shown)
        // 알림 권한이 없으면 본 것으로 치지 않는다. 나중에 허용하면 그때 뜬다
        if (fresh.isNotEmpty() && !notify(context, fresh)) return 0
        Prefs.setShownAlerts(context, shownAfter(alerts, shown, fresh))
        return fresh.size
    }

    // 예: 어머님의 기억력 기록이 평소보다 낮아요. 누르면 보호자 화면
    private fun notify(context: Context, alerts: List<Summary.Alert>): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false

        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notify_channel))
                .build()
        )
        val areas = alerts.mapNotNull { GameType.fromName(it.area)?.label }.distinct().joinToString(", ")
        val title = context.getString(R.string.notify_alert, Prefs.getElderCallName(context), areas)
        val detail = context.getString(R.string.notify_alert_detail)
        val open = Intent(context, GuardianHomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFY_ID, notification)
        return true
    }
}

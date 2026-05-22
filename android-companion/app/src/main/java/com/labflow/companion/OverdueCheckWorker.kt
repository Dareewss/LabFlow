package com.labflow.companion

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate

class OverdueCheckWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val settings = SettingsStore(appContext)
    private val prefs = appContext.getSharedPreferences("labflow_companion", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        if (!settings.notificationsEnabled || !settings.isLoggedIn || settings.host.isBlank() || settings.apiKey.isBlank()) {
            return Result.success()
        }

        return try {
            NotificationChannelManager.createChannels(appContext)
            val response = ApiClient.create(settings.host, settings.port)
                .home(settings.authorization(), settings.userId, if (settings.currentLabId > 0) settings.currentLabId else 0)
            val body = response.body()
            val home = if (response.isSuccessful && body?.success == true) body.data else null
            if (home != null) {
                notifyOverdue(home)
                notifyMaintenance(home)
                notifyFaults(home)
                notifyHealth(home)
            }
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    private fun notifyOverdue(home: CompanionHomeDto) {
        val overdue = home.borrowedItems.orEmpty().filter {
            runCatching { it.expectedReturnDate?.let(LocalDate::parse)?.isBefore(LocalDate.now()) == true }.getOrDefault(false)
        }
        if (overdue.isEmpty()) return
        val item = overdue.first()
        val signature = overdue.joinToString("|") { "${it.borrowRecordId}:${it.expectedReturnDate}" }
        if (signature == prefs.getString("notif_overdue_signature", null)) return
        prefs.edit().putString("notif_overdue_signature", signature).apply()
        post(
            id = 2101,
            channel = NotificationChannelManager.CHANNEL_OVERDUE,
            title = item.name.orEmpty().ifBlank { "Borrowed equipment" } + " is overdue",
            message = "Please return it as soon as possible.",
            targetTab = "BORROWS"
        )
    }

    private fun notifyMaintenance(home: CompanionHomeDto) {
        val count = home.stats?.maintenanceDueCount ?: 0
        if (count <= 0) return
        val signature = "${home.lab?.id}:$count"
        if (signature == prefs.getString("notif_maintenance_signature", null)) return
        prefs.edit().putString("notif_maintenance_signature", signature).apply()
        post(
            id = 2102,
            channel = NotificationChannelManager.CHANNEL_MAINTENANCE,
            title = "Maintenance due soon",
            message = "$count equipment item(s) need maintenance attention.",
            targetTab = "HOME"
        )
    }

    private fun notifyFaults(home: CompanionHomeDto) {
        val isTechnician = settings.role.equals("TECHNICIAN", ignoreCase = true)
        val count = home.stats?.faultCount ?: 0
        if (!isTechnician || count <= 0) return
        val signature = "${home.lab?.id}:$count"
        if (signature == prefs.getString("notif_fault_signature", null)) return
        prefs.edit().putString("notif_fault_signature", signature).apply()
        post(
            id = 2103,
            channel = NotificationChannelManager.CHANNEL_FAULTS,
            title = "Fault queue needs attention",
            message = "$count open fault report(s) are waiting in ${home.lab?.name.orEmpty().ifBlank { "your lab" }}.",
            targetTab = "HOME"
        )
    }

    private fun notifyHealth(home: CompanionHomeDto) {
        val score = home.stats?.healthScore ?: 100
        if (score >= 50) return
        val signature = "${home.lab?.id}:$score"
        if (signature == prefs.getString("notif_health_signature", null)) return
        prefs.edit().putString("notif_health_signature", signature).apply()
        post(
            id = 2104,
            channel = NotificationChannelManager.CHANNEL_HEALTH,
            title = "Lab health dropped to $score%",
            message = "Open the dashboard to review overdue items, faults, and maintenance risk.",
            targetTab = "HOME"
        )
    }

    private fun post(id: Int, channel: String, title: String, message: String, targetTab: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(appContext, MainActivity::class.java).apply {
            putExtra("targetTab", targetTab)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(appContext, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(appContext).notify(id, notification)
    }
}

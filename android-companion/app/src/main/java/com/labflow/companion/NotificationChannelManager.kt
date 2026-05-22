package com.labflow.companion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannelManager {
    const val CHANNEL_OVERDUE = "labflow_overdue"
    const val CHANNEL_MAINTENANCE = "labflow_maintenance"
    const val CHANNEL_FAULTS = "labflow_faults"
    const val CHANNEL_HEALTH = "labflow_health"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(
                CHANNEL_OVERDUE,
                "Overdue borrows",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Overdue borrowed equipment alerts." },
            NotificationChannel(
                CHANNEL_MAINTENANCE,
                "Maintenance reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Maintenance due soon alerts." },
            NotificationChannel(
                CHANNEL_FAULTS,
                "Fault assignments",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Fault-related technician alerts." },
            NotificationChannel(
                CHANNEL_HEALTH,
                "Lab health",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Laboratory health score updates." }
        )
        manager.createNotificationChannels(channels)
    }
}

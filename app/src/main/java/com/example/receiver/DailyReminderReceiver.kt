package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.util.ReminderScheduler

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule alarm on device restart if enabled
            if (ReminderScheduler.isReminderEnabled(context)) {
                ReminderScheduler.scheduleDailyReminder(context)
            }
            return
        }

        // Show friendly daily reminder notification
        showReminderNotification(context)

        // Reschedule for next day
        ReminderScheduler.scheduleDailyReminder(context)
    }

    private fun showReminderNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "channel_daily_reminder"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pengingat Belanja Harian",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat mencatat pengeluaran belanja harian sebelum jam 9 malam"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_QUICK_ADD", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Bunda, Catat Belanja Hari Ini Yuk! 🥬")
            .setContentText("Luangkan 1 menit sebelum jam 9 malam agar pos amplop anggaran dapur tetap aman terkontrol.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Luangkan 1 menit sebelum jam 9 malam untuk mencatat pengeluaran dapur & jajan hari ini. Amplop belanja rapi, hati tenang! 💖")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}

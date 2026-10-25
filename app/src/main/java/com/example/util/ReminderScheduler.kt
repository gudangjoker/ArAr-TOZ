package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.receiver.DailyReminderReceiver
import java.util.Calendar

object ReminderScheduler {
    private const val PREFS_NAME = "dompet_prefs"
    private const val KEY_REMINDER_ENABLED = "daily_reminder_enabled"
    private const val KEY_REMINDER_HOUR = "daily_reminder_hour"
    private const val KEY_REMINDER_MINUTE = "daily_reminder_minute"

    private const val DEFAULT_HOUR = 20 // 20:30 WIB (sebelum jam 9 malam)
    private const val DEFAULT_MINUTE = 30
    private const val ALARM_REQ_CODE = 2001

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isReminderEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMINDER_ENABLED, true)
    }

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
        if (enabled) {
            scheduleDailyReminder(context)
        } else {
            cancelDailyReminder(context)
        }
    }

    fun getReminderTime(context: Context): Pair<Int, Int> {
        val prefs = getPrefs(context)
        val hour = prefs.getInt(KEY_REMINDER_HOUR, DEFAULT_HOUR)
        val minute = prefs.getInt(KEY_REMINDER_MINUTE, DEFAULT_MINUTE)
        return Pair(hour, minute)
    }

    fun setReminderTime(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
        if (isReminderEnabled(context)) {
            scheduleDailyReminder(context)
        }
    }

    fun scheduleDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val (hour, minute) = getReminderTime(context)

        val calendar = Calendar.getInstance(DateTimeUtils.jakartaTimeZone).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If time has already passed today in Jakarta, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = "com.example.ACTION_DAILY_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Inexact fallback if exact alarm permission isn't granted
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = "com.example.ACTION_DAILY_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

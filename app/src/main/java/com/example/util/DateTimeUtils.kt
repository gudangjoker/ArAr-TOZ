package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {
    /**
     * Resolves the user's active device timezone dynamically.
     * Supports all Indonesian regions: WIB (UTC+7), WITA (UTC+8), WIT (UTC+9), and international timezones.
     */
    val currentDeviceTimeZone: TimeZone
        get() = TimeZone.getDefault()

    val jakartaTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    private val indonesianLocale = Locale.forLanguageTag("id-ID")

    fun getCurrentJakartaTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Returns standard Indonesian timezone abbreviation (WIB, WITA, WIT) or localized abbreviation.
     */
    fun getTimeZoneAbbreviation(tz: TimeZone = currentDeviceTimeZone): String {
        return when (tz.rawOffset / (3600 * 1000)) {
            7 -> "WIB"
            8 -> "WITA"
            9 -> "WIT"
            else -> tz.getDisplayName(false, TimeZone.SHORT, indonesianLocale)
        }
    }

    /**
     * Start of day in local device time (00:00:00.000)
     */
    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(currentDeviceTimeZone)
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * End of day in local device time (23:59:59.999)
     */
    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(currentDeviceTimeZone)
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /**
     * Start of current month in local device time
     */
    fun getStartOfMonth(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(currentDeviceTimeZone)
        cal.timeInMillis = timestamp
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * End of current month in local device time
     */
    fun getEndOfMonth(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(currentDeviceTimeZone)
        cal.timeInMillis = timestamp
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /**
     * Formats timestamp into: "Kamis, 24 Sep 2026"
     */
    fun formatDateWithDay(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE, d MMM yyyy", indonesianLocale)
        sdf.timeZone = currentDeviceTimeZone
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats timestamp into: "24 Sep 2026, 14:30 WIB" (or WITA / WIT dynamically)
     */
    fun formatDateTimeJakarta(timestamp: Long): String {
        val tzSuffix = getTimeZoneAbbreviation()
        val sdf = SimpleDateFormat("d MMM yyyy, HH:mm '$tzSuffix'", indonesianLocale)
        sdf.timeZone = currentDeviceTimeZone
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats current month name: "September 2026"
     */
    fun formatCurrentMonth(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("MMMM yyyy", indonesianLocale)
        sdf.timeZone = currentDeviceTimeZone
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats short date: "24 Sep"
     */
    fun formatShortDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("d MMM", indonesianLocale)
        sdf.timeZone = currentDeviceTimeZone
        return sdf.format(Date(timestamp))
    }

    /**
     * Current month key for tracking monthly bill payments, e.g. "09-2026"
     */
    fun getCurrentMonthYearKey(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("MM-yyyy", indonesianLocale)
        sdf.timeZone = currentDeviceTimeZone
        return sdf.format(Date(timestamp))
    }

    /**
     * Start of last month in local device time
     */
    fun getStartOfLastMonth(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(currentDeviceTimeZone)
        cal.timeInMillis = timestamp
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * End of last month in local device time
     */
    fun getEndOfLastMonth(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(currentDeviceTimeZone)
        cal.timeInMillis = timestamp
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}

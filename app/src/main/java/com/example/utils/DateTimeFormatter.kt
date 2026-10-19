package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeFormatter {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val monthDayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    fun formatNoteDate(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val now = Calendar.getInstance()
        val noteTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        val diffMillis = now.timeInMillis - timestamp
        if (diffMillis in 0..60_000) {
            return "Just now"
        }

        val isSameDay = now.get(Calendar.YEAR) == noteTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == noteTime.get(Calendar.DAY_OF_YEAR)
        if (isSameDay) {
            return timeFormat.format(Date(timestamp))
        }

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == noteTime.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == noteTime.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) {
            return "Yesterday • ${timeFormat.format(Date(timestamp))}"
        }

        val isSameYear = now.get(Calendar.YEAR) == noteTime.get(Calendar.YEAR)
        return if (isSameYear) {
            "${monthDayFormat.format(Date(timestamp))} • ${timeFormat.format(Date(timestamp))}"
        } else {
            fullDateTimeFormat.format(Date(timestamp))
        }
    }
}

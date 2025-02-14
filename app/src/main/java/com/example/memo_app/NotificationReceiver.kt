package com.example.memo_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationReceiver", "Daily notification triggered")
        val noteDao = NoteDao(context)
        val today = Calendar.getInstance()

        val notes = noteDao.getAllNotes().filter {
            val noteDate = dateTimeFormat.parse(it.dateTime)
            isSameDay(today, Calendar.getInstance().apply { time = noteDate })
        }

        if (notes.isNotEmpty()) {
            val notificationHelper = NotificationHelper(context)
            notificationHelper.sendNotification(
                "Планы на сегодня",
                "У вас есть запланированные события на сегодня."
            )
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
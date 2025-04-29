package com.example.memo_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteDao = NoteDao(context)

        // Получаем список всех заметок
        val allNotes = noteDao.getAllNotes()

        // Фильтруем заметки: активные с датой и без даты
        val activeNotesWithDate = allNotes.filter { !it.isDeleted && !it.dateTime.isNullOrBlank() }
        val notesWithoutDate = allNotes.filter { !it.isDeleted && it.dateTime.isNullOrBlank() }

        // Уведомление для задач с датой
        if (activeNotesWithDate.isNotEmpty()) {
            val blockName = activeNotesWithDate.firstOrNull()?.content ?: "Нет подходящего блока"
            val notificationHelper = NotificationHelper(context)
            notificationHelper.sendNotification(
                "\uD83D\uDD14 Похоже у вас планы!",
                "$blockName"
            )
            Log.d("NotificationReceiver", "Уведомление отправлено с названием блока: $blockName")
        } else {
            Log.d("NotificationReceiver", "Нет активных задач с указанием времени")
        }

        // Уведомление для дел без даты — отправляем только один раз в день
        if (notesWithoutDate.isNotEmpty()) {
            val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            val lastSentDate = prefs.getString("no_date_notif_date", null)
            val currentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

            if (lastSentDate != currentDate) {
                val notificationHelper = NotificationHelper(context)
                notificationHelper.sendNotification(
                    "\uD83D\uDD14 Похоже у вас планы!",
                    "Проверьте записи, которые оставили ранее"
                )
                prefs.edit().putString("no_date_notif_date", currentDate).apply() // Сохраняем текущую дату
                Log.d("NotificationReceiver", "Уведомление без даты отправлено: $currentDate")
            } else {
                Log.d("NotificationReceiver", "Уведомление без даты уже отправлялось сегодня")
            }
        } else {
            Log.d("NotificationReceiver", "Нет дел без даты")
        }
    }
}
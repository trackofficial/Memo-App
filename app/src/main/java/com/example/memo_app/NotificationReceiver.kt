package com.example.memo_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteDao = NoteDao(context)

        // Проверяем наличие активных блоков с датой
        val activeNotes = noteDao.getAllNotes().filter { !it.isDeleted && !it.dateTime.isNullOrBlank() }

        // Проверяем наличие блоков без даты
        val notesWithoutDate = noteDao.getAllNotes().filter { !it.isDeleted && it.dateTime.isNullOrBlank() }

        // Отправляем уведомления для задач с датой
        if (activeNotes.isNotEmpty()) {
            val blockName = activeNotes.firstOrNull()?.content ?: "Нет подходящего блока"
            val notificationHelper = NotificationHelper(context)
            notificationHelper.sendNotification(
                "Напоминание",
                "У вас есть задача: $blockName"
            )
            Log.d("NotificationReceiver", "Уведомление отправлено с названием блока: $blockName")
        } else {
            Log.d("NotificationReceiver", "Нет активных задач с указанием времени")
        }

        // Отправляем уведомление для блоков без даты (раз в день)
        if (notesWithoutDate.isNotEmpty()) {
            val notificationHelper = NotificationHelper(context)
            notificationHelper.sendNotification(
                "Напоминание",
                "У вас есть дело без даты!"
            )
            Log.d("NotificationReceiver", "Уведомление отправлено для блоков без даты")
        } else {
            Log.d("NotificationReceiver", "Нет дел без даты")
        }
    }
}
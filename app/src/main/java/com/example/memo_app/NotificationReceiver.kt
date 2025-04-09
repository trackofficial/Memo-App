package com.example.memo_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Проверяем наличие активных заметок (не удалённых) и их времени выполнения.
        val noteDao = NoteDao(context)
        val activeNotes = noteDao.getAllNotes().filter { !it.isDeleted && !it.dateTime.isNullOrBlank() }

        if (activeNotes.isEmpty()) {
            Log.d("NotificationReceiver", "Нет активных задач с указанием времени – уведомление не отправляется.")
            return
        }

        // Получаем сообщение, переданное через PendingIntent.
        val message = intent.getStringExtra("notification_message") ?: "Напоминание: Проверьте свои планы"

        // Добавляем название первого подходящего блока
        val blockName = activeNotes.firstOrNull()?.content ?: "Нет подходящего блока"

        // Отправляем уведомление
        val notificationHelper = NotificationHelper(context)
        notificationHelper.sendNotification("Напоминание", "$message\nБлок: $blockName")

        Log.d("NotificationReceiver", "Уведомление отправлено с сообщением: $message и названием блока: $blockName")
    }
}
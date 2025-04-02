package com.example.memo_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Проверяем наличие активных заметок (не удалённых).
        val noteDao = NoteDao(context)
        val activeNotes = noteDao.getAllNotes().filter { !it.isDeleted }
        if (activeNotes.isEmpty()) {
            Log.d("NotificationReceiver", "Нет активных задач – уведомление не отправляется.")
            return
        }

        // Получаем сообщение, переданное через PendingIntent.
        val message = intent.getStringExtra("notification_message") ?: "Напоминание: Проверьте свои планы"

        // Добавляем название первого блока (если доступно)
        val blockName = activeNotes.firstOrNull()?.content ?: "Нет названия блока"

        // Получаем экземпляр NotificationHelper и отправляем уведомление.
        val notificationHelper = NotificationHelper(context)
        notificationHelper.sendNotification("Напоминание", "$message\nБлок: $blockName")

        Log.d("NotificationReceiver", "Уведомление отправлено с сообщением: $message и названием блока: $blockName")
    }
}
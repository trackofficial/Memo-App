package com.example.memo_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Получаем сообщение, переданное через PendingIntent.
        // Если оно не передано, используется значение по умолчанию.
        val message = intent.getStringExtra("notification_message")
            ?: "Напоминание: Проверьте свои планы"

        // Получаем экземпляр NotificationHelper и отправляем уведомление.
        val notificationHelper = NotificationHelper(context)
        notificationHelper.sendNotification("Напоминание", message)

        Log.d("NotificationReceiver", "Notification fired with message: $message")
    }
}
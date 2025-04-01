package com.example.memo_app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "memo_app_channel"
        private const val CHANNEL_NAME = "Memo App Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for Memo App"
        private const val NOTIFICATION_ID = 1
    }

    init {
        createNotificationChannel()
    }

    // Создание канала уведомлений для Android O и выше.
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Отправка уведомления с указанным заголовком и сообщением.
    fun sendNotification(title: String, message: String) {
        // Проверяем разрешение для уведомлений (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("NotificationHelper", "Permission for notifications not granted")
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.pin_for_notif)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            // Используем уникальный ID для уведомления,
            // чтобы каждое уведомление отображалось отдельно.
            NotificationManagerCompat.from(context.applicationContext)
                .notify((System.currentTimeMillis() / 1000).toInt(), notification)
            Log.d("NotificationHelper", "Notification sent: $title - $message")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error sending notification", e)
        }
    }

    /**
     * Планирование уведомлений для задачи (плана) с заданным временем (planTime – в миллисекундах).
     *
     * Логика:
     * • Если до плана более 48 часов: каждое утро в 9:00 изначально планируются уведомления (раз в день)
     *   до момента, когда остаётся 48 часов.
     * • Если от 24 до 48 часов до плана: планируются два уведомления в день (например, в 10:00 и 16:00) за 1 день до плана.
     * • Если осталось менее 24 часов: уведомления будут приходить каждые 2 часа до наступления плана.
     */
    fun schedulePlanNotifications(planTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentTime = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        if (planTime <= currentTime) {
            Log.d("NotificationHelper", "Plan time is in the past. No notifications scheduled.")
            return
        }

        // Более 48 часов до плана: уведомления каждый день в 9:00
        if (planTime - currentTime > 2 * dayMs) {
            var calendar = Calendar.getInstance().apply {
                timeInMillis = currentTime
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            // Если сегодняшний 9:00 уже прошёл, переходим к следующему дню.
            if (calendar.timeInMillis < currentTime) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            // Планируем уведомления до момента, когда останется 48 часов до плана.
            val endTime = planTime - 2 * dayMs
            while (calendar.timeInMillis <= endTime) {
                scheduleSingleNotification(alarmManager, calendar.timeInMillis, "Напоминание: у вас задача запланирована на будущее")
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        // От 24 до 48 часов до плана: два уведомления за день (например, в 10:00 и 16:00)
        else if (planTime - currentTime > dayMs) {
            var calendar = Calendar.getInstance()
            calendar.timeInMillis = planTime - dayMs   // день до плана
            calendar.set(Calendar.HOUR_OF_DAY, 10)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val firstTime = calendar.timeInMillis
            if (firstTime > currentTime) {
                scheduleSingleNotification(alarmManager, firstTime, "Напоминание: Остался 1 день до задачи")
            }
            calendar.set(Calendar.HOUR_OF_DAY, 16)
            val secondTime = calendar.timeInMillis
            if (secondTime > currentTime) {
                scheduleSingleNotification(alarmManager, secondTime, "Напоминание: Остался 1 день до задачи")
            }
        }
        // Менее 24 часов до плана: уведомления каждые 2 часа
        else {
            var nextTime = currentTime + 2 * 60 * 60 * 1000L // через 2 часа от текущего времени
            while (nextTime < planTime) {
                scheduleSingleNotification(alarmManager, nextTime, "Напоминание: задача сегодня!")
                nextTime += 2 * 60 * 60 * 1000L
            }
        }
        Log.d("NotificationHelper", "Plan notifications scheduled for planTime: $planTime")
    }

    // Планирование одного уведомления с указанным запуском (triggerTime) и сообщением.
    private fun scheduleSingleNotification(
        alarmManager: AlarmManager,
        triggerTime: Long,
        message: String
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notification_message", message)
        }
        // Используем время запуска (в секундах) как уникальный requestCode.
        val requestCode = (triggerTime / 1000).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        Log.d("NotificationHelper", "Scheduled single notification at $triggerTime with message: $message")
    }

    // Старый метод планирования ежедневного уведомления (если требуется оставить)
    fun scheduleDailyNotification() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, NotificationReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            alarmIntent
        )
        Log.d("NotificationHelper", "Daily notification scheduled at 9:00 AM")
    }
}
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    //.
    fun schedulePlanNotifications(planTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentTime = System.currentTimeMillis()

        if (planTime <= currentTime) {
            Log.d("NotificationHelper", "Plan time is in the past. No notifications scheduled.")
            return
        }

        // Проверяем, есть ли вообще заметки
        val noteDao = NoteDao(context)
        val allNotes = noteDao.getAllNotes()
        if (allNotes.isEmpty()) {
            Log.d("NotificationHelper", "No notes found. Notifications will not be scheduled.")
            return
        }

        // Проверяем, есть ли заметки на конкретный день
        val notes = allNotes.filter {
            try {
                val noteDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(it.dateTime)
                if (noteDateTime != null) {
                    isSameDay(
                        Calendar.getInstance().apply { timeInMillis = planTime },
                        Calendar.getInstance().apply { time = noteDateTime }
                    )
                } else {
                    false // Если noteDateTime == null, исключаем заметку из списка
                }
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Ошибка при парсинге даты: ${it.dateTime}", e)
                false // Исключаем заметку, если произошла ошибка
            }
        }

        if (notes.isEmpty()) {
            Log.d("NotificationHelper", "No plans found for the specified date. No notifications scheduled.")
            return
        }

        val dayMs = 24 * 60 * 60 * 1000L

        // Более 48 часов до плана: уведомления каждый день
        if (planTime - currentTime > 2 * dayMs) {
            var calendar = Calendar.getInstance().apply {
                timeInMillis = currentTime
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            if (calendar.timeInMillis < currentTime) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            val endTime = planTime - 2 * dayMs
            while (calendar.timeInMillis <= endTime) {
                scheduleSingleNotification(alarmManager, calendar.timeInMillis, "Напоминание: задача через несколько дней")
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // От 24 до 48 часов: уведомления утром и вечером
        else if (planTime - currentTime > dayMs) {
            var calendar = Calendar.getInstance()
            calendar.timeInMillis = planTime - dayMs
            calendar.set(Calendar.HOUR_OF_DAY, 10)
            val morningTime = calendar.timeInMillis
            if (morningTime > currentTime) {
                scheduleSingleNotification(alarmManager, morningTime, "Напоминание: Остался 1 день до выполнения задачи")
            }
            calendar.set(Calendar.HOUR_OF_DAY, 18)
            val eveningTime = calendar.timeInMillis
            if (eveningTime > currentTime) {
                scheduleSingleNotification(alarmManager, eveningTime, "Напоминание: Остался 1 день до выполнения задачи")
            }
        }

        // Менее 24 часов: утром, в обед, и за час до выполнения
        else {
            val morningTime = calculateTimeForHour(planTime - dayMs, 8) // 8:00 утра
            val noonTime = calculateTimeForHour(planTime - dayMs, 12)  // 12:00 дня
            val hourBeforeTime = planTime - 60 * 60 * 1000L            // За час до плана

            if (morningTime > currentTime) {
                scheduleSingleNotification(alarmManager, morningTime, "Напоминание: задача сегодня утром!")
            }

            if (noonTime > currentTime) {
                scheduleSingleNotification(alarmManager, noonTime, "Напоминание: задача сегодня в обед!")
            }

            if (hourBeforeTime > currentTime) {
                scheduleSingleNotification(alarmManager, hourBeforeTime, "Напоминание: задача уже совсем скоро!")
            }
        }
    }

    private fun calculateTimeForHour(baseTime: Long, hour: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = baseTime
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun scheduleSingleNotification(
        alarmManager: AlarmManager,
        triggerTime: Long,
        message: String
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notification_message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (triggerTime / 1000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        Log.d("NotificationHelper", "Scheduled single notification at $triggerTime with message: $message")
    }
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
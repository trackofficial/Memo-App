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
     fun schedulePlanNotifications() {
         val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
         val currentTime = System.currentTimeMillis()

         // Получаем все заметки и фильтруем только активные (не удалённые)
         val noteDao = NoteDao(context)
         val activeNotes = noteDao.getAllNotes().filter { !it.isDeleted }

         if (activeNotes.isEmpty()) {
             Log.d("NotificationHelper", "Нет активных заметок. Уведомления не запланированы.")
             cancelAllNotifications()  // Отменяем уже запланированные уведомления, если они существуют.
             return
         }

         // Парсим дату из каждой активной заметки и выбираем те, что в будущем
         val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
         val futureTimes = activeNotes.mapNotNull { note ->
             try {
                 val noteDate = sdf.parse(note.dateTime)
                 val time = noteDate?.time
                 if (time != null && time > currentTime) time else null
             } catch (e: Exception) {
                 Log.e("NotificationHelper", "Ошибка парсинга даты заметки: ${note.dateTime}", e)
                 null
             }
         }

         if (futureTimes.isEmpty()) {
             Log.d("NotificationHelper", "Нет предстоящих активных задач. Уведомления не запланированы.")
             cancelAllNotifications()
             return
         }

         // Выбираем ближайшее будущее время для плана
         val planTime = futureTimes.minOrNull()!!
         if (planTime <= currentTime) {
             Log.d("NotificationHelper", "Время плана в прошлом. Уведомления не запланированы.")
             cancelAllNotifications()
             return
         }

         val dayMs = 24 * 60 * 60 * 1000L

         // Если до плана более 48 часов: уведомления каждый день в 9:00
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
         // Если оставшийся промежуток от 24 до 48 часов: уведомления утром и вечером
         else if (planTime - currentTime > dayMs) {
             var calendar = Calendar.getInstance().apply { timeInMillis = planTime - dayMs }  // день до выполнения
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
         // Если до выполнения менее 24 часов: уведомления утром, в обед и за час до выполнения
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

         Log.d("NotificationHelper", "Уведомления запланированы для времени плана: $planTime")
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

    private fun scheduleSingleNotification(alarmManager: AlarmManager, triggerTime: Long, message: String) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notification_message", message)
        }
        val requestCode = (triggerTime / 1000).toInt()  // Используем время (в секундах) для уникальности
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        Log.d("NotificationHelper", "Запланировано уведомление на $triggerTime с сообщением: $message")
    }

    fun cancelAllNotifications() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Пробуем получить PendingIntent, созданные с разными requestCode. Если они существуют, отменяем их.
        // Здесь можно хранить список всех запланированных requestCode, чтобы точно отменить весь набор.
        // Ниже приведён пример отмены базового PendingIntent с requestCode = 0.
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("NotificationHelper", "Все запланированные уведомления отменены.")
        }
    }

}
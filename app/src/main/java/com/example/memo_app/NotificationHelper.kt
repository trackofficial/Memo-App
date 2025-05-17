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
    }

    init {
        createNotificationChannel()
    }

    // Создание канала уведомлений (для Android O и выше)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Отправка обычного уведомления с заголовком и сообщением
    fun sendNotification(title: String, message: String) {
        // Проверяем разрешения (для Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
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
            NotificationManagerCompat.from(context.applicationContext)
                .notify((System.currentTimeMillis() / 1000).toInt(), notification)
            Log.d("NotificationHelper", "Notification sent: $title - $message")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error sending notification", e)
        }
    }

    // Пример планирования ежедневного уведомления в 9:00 AM (по расписанию)
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

    // Планирование уведомлений для активных заметок (если есть задачи на сегодня)
    fun schedulePlanNotifications() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentTime = System.currentTimeMillis()

        // Получаем активные (не удалённые) заметки
        val noteDao = NoteDao(context)
        val activeNotes = noteDao.getAllNotes().filter { !it.isDeleted }

        if (activeNotes.isEmpty()) {
            Log.d("NotificationHelper", "Нет активных заметок. Уведомления не запланированы.")
            cancelAllNotifications()
            return
        }

        // Формат даты заметки: "yyyy-MM-dd HH:mm"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        // Получаем сегодняшнюю дату в формате "yyyy-MM-dd"
        val today = getTodayDate()

        // Проверяем, есть ли задачи именно на сегодня
        val hasTodayTasks = activeNotes.any { note ->
            try {
                val noteDate = sdf.parse(note.dateTime)
                noteDate?.let {
                    val noteDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                    noteDateStr == today
                } ?: false
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Ошибка парсинга даты заметки: ${note.dateTime}", e)
                false
            }
        }

        if (!hasTodayTasks) {
            Log.d("NotificationHelper", "Сегодня нет запланированных задач.")
            return
        }

        // Планируем утреннее уведомление в 08:00 и вечернее уведомление в 20:00
        val morningTime = calculateTimeForHour(currentTime, 8)
        val eveningTime = calculateTimeForHour(currentTime, 20)

        if (morningTime > currentTime) {
            scheduleSingleNotification(alarmManager, morningTime, "Напоминание: запланированные задачи на сегодня!")
        }
        if (eveningTime > currentTime) {
            scheduleSingleNotification(alarmManager, eveningTime, "Напоминание: проверьте выполненные задачи!")
        }

        Log.d("NotificationHelper", "Уведомления запланированы для сегодня. Текущее время: $currentTime")
    }

    // Возвращает сегодняшнюю дату в формате "yyyy-MM-dd"
    private fun getTodayDate(): String {
        val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return todayFormat.format(System.currentTimeMillis())
    }

    // Вычисляет время на основе базового времени для указанного часа (например, 8:00 или 20:00).
    // Если указанное время уже прошло сегодня, уведомление запланируется на следующий день.
    private fun calculateTimeForHour(baseTime: Long, targetHour: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = baseTime
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis < baseTime) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    // Планирует одиночное уведомление на заданное время с указанным сообщением
    private fun scheduleSingleNotification(alarmManager: AlarmManager, triggerTime: Long, message: String) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notification_message", message)
        }
        // Используем triggerTime (в секундах) как уникальный requestCode
        val requestCode = (triggerTime / 1000).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        Log.d("NotificationHelper", "Запланировано уведомление на $triggerTime с сообщением: $message")
    }

    // Отменяет все запланированные уведомления (пример – снимается базовый PendingIntent).
    // Для точного контроля можно хранить список всех requestCode.
    fun cancelAllNotifications() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
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

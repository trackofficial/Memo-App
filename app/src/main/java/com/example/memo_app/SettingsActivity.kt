package com.example.memo_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import com.example.memo_app.NoteDao

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity) // Подключаем макет

        // Инициализируем кнопку "Очистить историю"
        val clearHistoryButton = findViewById<Button>(R.id.clearHistoryButton)

        clearHistoryButton.setOnClickListener {
            showCustomDialog() // Показываем кастомное диалоговое окно
        }
    }

    private fun showCustomDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom, null)

        // Получаем элементы из кастомного макета
        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        // Создаём AlertDialog с кастомным макетом
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        // Устанавливаем закругление для окна диалога
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        // Обработчик кнопки подтверждения
        confirmButton.setOnClickListener {
            clearHistory() // Удаляем историю
            dialog.dismiss() // Закрываем диалог
        }

        // Обработчик кнопки отмены
        cancelButton.setOnClickListener {
            dialog.dismiss() // Закрываем диалог без действий
        }

        dialog.show() // Показываем диалог
    }

    private fun clearHistory() {
        val noteDao = NoteDao(this) // Инициализация DAO
        noteDao.clearHistory() // Удаляем записи, где isDeleted = true

        Toast.makeText(this, "История успешно очищена!", Toast.LENGTH_SHORT).show()

        // Переход на главный экран
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Завершаем текущую активность
    }
}
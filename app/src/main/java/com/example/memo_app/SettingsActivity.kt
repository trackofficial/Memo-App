package com.example.memo_app

import android.content.Intent
import android.os.Bundle
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageButton
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
            animateButtonClick(clearHistoryButton)
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
            animateButtonClick(confirmButton)
            clearHistory() // Удаляем историю
            dialog.dismiss() // Закрываем диалог
        }

        // Обработчик кнопки отмены
        cancelButton.setOnClickListener {
            animateButtonClick(cancelButton)
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
    fun animateButtonClick(button: Button) {
        // Анимация уменьшения кнопки
        val scaleDown = ScaleAnimation(
            1.0f, 0.95f,  // Уменьшение ширины
            1.0f, 0.95f,  // Уменьшение высоты
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,  // Точка опоры по X
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f   // Точка опоры по Y
        )
        scaleDown.duration = 60 // Продолжительность анимации в миллисекундах
        scaleDown.fillAfter = true // Кнопка остаётся в уменьшенном состоянии до завершения

        // Возвращаем к исходному размеру
        scaleDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                val scaleUp = ScaleAnimation(
                    0.95f, 1.0f,  // Увеличение ширины обратно
                    0.95f, 1.0f,  // Увеличение высоты обратно
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                )
                scaleUp.duration = 50
                scaleUp.fillAfter = true
                button.startAnimation(scaleUp) // Запуск обратной анимации
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        button.startAnimation(scaleDown) // Запуск первой анимации
    }
}
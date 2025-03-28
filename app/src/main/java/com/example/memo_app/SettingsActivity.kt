package com.example.memo_app

import android.os.Bundle
import android.widget.Button
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity


class SettingsActivity : ComponentActivity() {

    private lateinit var clearHistoryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        clearHistoryButton = findViewById(R.id.clearHistoryButton)

        clearHistoryButton.setOnClickListener {
            clearHistory()
        }
    }

    private fun clearHistory() {
        val noteDao = NoteDao(this) // Инициализация DAO
        noteDao.clearHistory() // Удаляем записи с флагом isDeleted = true

        Toast.makeText(this, "История успешно очищена!", Toast.LENGTH_SHORT).show()

        // Переход на главный экран
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
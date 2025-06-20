package com.example.memo_app

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity

class TimerSettingsActivity : ComponentActivity() {

    private lateinit var editMainDuration: EditText
    private lateinit var editBreakDuration: EditText
    private lateinit var editCyclesTarget: EditText
    private lateinit var saveButton: Button

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.timer_settings)

        prefs = getSharedPreferences("timer_settings", MODE_PRIVATE)

        editMainDuration = findViewById(R.id.edit_main_duration)
        editBreakDuration = findViewById(R.id.edit_break_duration)
        editCyclesTarget = findViewById(R.id.edit_cycles_target)
        saveButton = findViewById(R.id.save_timer_settings_button)

        // Загружаем текущие настройки (переводим минуты в число)
        val mainDuration = prefs.getLong("main_duration", 25 * 60 * 1000L) / 60000
        val breakDuration = prefs.getLong("break_duration", 5 * 60 * 1000L) / 60000
        val cyclesTarget = prefs.getInt("cycles_target", 4)

        editMainDuration.setText(mainDuration.toString())
        editBreakDuration.setText(breakDuration.toString())
        editCyclesTarget.setText(cyclesTarget.toString())

        saveButton.setOnClickListener {
            // Сохраняем введённые пользователем значения (переводим минуты обратно в миллисекунды)
            val newMainDuration = (editMainDuration.text.toString().toLongOrNull() ?: 25) * 60 * 1000L
            val newBreakDuration = (editBreakDuration.text.toString().toLongOrNull() ?: 5) * 60 * 1000L
            val newCyclesTarget = editCyclesTarget.text.toString().toIntOrNull() ?: 4

            prefs.edit()
                .putLong("main_duration", newMainDuration)
                .putLong("break_duration", newBreakDuration)
                .putInt("cycles_target", newCyclesTarget)
                .apply()
            finish()
        }
    }
}
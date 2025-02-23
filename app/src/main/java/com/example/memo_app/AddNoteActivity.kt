package com.example.memo_app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import java.util.Calendar

class AddNoteActivity : ComponentActivity() {

    private lateinit var editTextNoteContent: EditText
    private lateinit var editTextDescription: EditText // Новое поле для описания
    private lateinit var editTextTime: EditText
    private lateinit var buttonSaveNote: ImageButton
    private lateinit var buttonSelectDate: ImageButton
    private lateinit var noteDao: NoteDao
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        editTextNoteContent = findViewById(R.id.editTextNoteContent)
        editTextDescription = findViewById(R.id.editAddText) // Инициализация нового поля
        editTextTime = findViewById(R.id.editTextTime)
        buttonSaveNote = findViewById(R.id.buttonSave)
        buttonSelectDate = findViewById(R.id.buttonSelectDateTime)
        noteDao = NoteDao(this)

        buttonSelectDate.setOnClickListener {
            selectDate()
        }

        editTextTime.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.length == 4) {
                    val hour = s.substring(0, 2)
                    val minute = s.substring(2, 4)
                    editTextTime.setText("$hour:$minute")
                    editTextTime.setSelection(editTextTime.text.length)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        buttonSaveNote.setOnClickListener {
            val noteContent = editTextNoteContent.text.toString()
            val noteDescription = editTextDescription.text.toString().takeIf { it.isNotEmpty() } ?: "-" // Получение описания
            val time = editTextTime.text.toString().takeIf { it.isNotEmpty() }?.let {
                if (it.length == 4) {
                    "${it.substring(0, 2)}:${it.substring(2, 4)}"
                } else {
                    it
                }
            } ?: "-" // Обработка времени

            Log.d("AddNoteActivity", "Note content: $noteContent, description: $noteDescription, time: $time")

            if (noteContent.isNotEmpty()) {
                val dateTime = if (selectedDate.isNotEmpty() && time != "-") {
                    "$selectedDate $time"
                } else {
                    selectedDate.takeIf { it.isNotEmpty() } ?: "-"
                }

                val note = Note(
                    id = 0, // ID будет генерироваться автоматически
                    content = noteContent,
                    description = noteDescription, // Установка описания
                    dateTime = dateTime // Установка времени
                )

                noteDao.insert(note)
                Log.d("AddNoteActivity", "Note added: $note")

                // Переход на главный экран
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                if (noteContent.isEmpty()) {
                    editTextNoteContent.error = "Текст не может быть пустым"
                }
            }
        }
    }
    private fun selectDate() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            selectedDate = "$year-${month + 1}-$dayOfMonth"
            Log.d("AddNoteActivity", "Selected date: $selectedDate")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }
}
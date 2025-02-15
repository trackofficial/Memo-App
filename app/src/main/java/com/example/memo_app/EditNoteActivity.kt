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

class EditNoteActivity : ComponentActivity() {

    private lateinit var editTextNoteContent: EditText
    private lateinit var editTextDescription: EditText // Новое поле для описания
    private lateinit var editTextTime: EditText
    private lateinit var buttonDeleteNote: ImageButton
    private lateinit var buttonSaveNote: ImageButton
    private lateinit var buttonSelectDate: ImageButton
    private lateinit var noteDao: NoteDao
    private var noteId: Int = 0
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        editTextNoteContent = findViewById(R.id.editTextNoteContent)
        editTextDescription = findViewById(R.id.editAddText) // Инициализация нового поля
        editTextTime = findViewById(R.id.editTextTime)
        buttonDeleteNote = findViewById(R.id.buttonDeleteNote)
        buttonSaveNote = findViewById(R.id.buttonSaveNote)
        buttonSelectDate = findViewById(R.id.buttonSelectDateTime)
        noteDao = NoteDao(this)

        // Логирование инициализации
        noteId = intent.getIntExtra("noteId", 0)
        Log.d("EditNoteActivity", "Initializing with noteId: $noteId")

        val note = noteDao.getAllNotesIncludingDeleted().firstOrNull { it.id == noteId }

        if (note != null) {
            Log.d("EditNoteActivity", "Note loaded: $note")
            editTextNoteContent.setText(note.content)
            editTextDescription.setText(note.description) // Заполнение поля описания
            val dateTimeParts = note.dateTime.split(" ")
            if (dateTimeParts.size == 2) {
                selectedDate = dateTimeParts[0]
                editTextTime.setText(dateTimeParts[1].replace(":", ""))
            }
        } else {
            Log.d("EditNoteActivity", "Note not found")
        }

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

        buttonDeleteNote.setOnClickListener {
            Log.d("EditNoteActivity", "Delete button clicked")
            if (note != null) {
                note.isDeleted = true
                noteDao.update(note)
                Log.d("EditNoteActivity", "Note marked as deleted: $note")

                // Переход на главный экран
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }

        buttonSaveNote.setOnClickListener {
            Log.d("EditNoteActivity", "Save button clicked")
            val updatedContent = editTextNoteContent.text.toString()
            val updatedDescription = editTextDescription.text.toString() // Получение описания
            val time = editTextTime.text.toString()
            Log.d("EditNoteActivity", "Updated content: $updatedContent, description: $updatedDescription")
            if (note != null && updatedContent.isNotEmpty() && selectedDate.isNotEmpty() && time.isNotEmpty()) {
                // Обработка времени в формате HH:mm
                val timeFormatted = if (time.length == 4) {
                    "${time.substring(0, 2)}:${time.substring(2, 4)}"
                } else {
                    time
                }
                val dateTime = "$selectedDate $timeFormatted"
                note.content = updatedContent
                note.description = updatedDescription // Обновление описания
                note.dateTime = dateTime
                note.isDeleted = false // Убедитесь, что заметка не помечена как удаленная
                noteDao.update(note)
                Log.d("EditNoteActivity", "Note updated: $note")

                // Переход на главный экран
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                if (note == null) {
                    Log.d("EditNoteActivity", "Note is null")
                }
                if (updatedContent.isEmpty()) {
                    Log.d("EditNoteActivity", "Updated content is empty")
                    editTextNoteContent.error = "Текст не может быть пустым"
                }
                if (selectedDate.isEmpty()) {
                    Log.d("EditNoteActivity", "Date is empty")
                }
                if (time.isEmpty()) {
                    Log.d("EditNoteActivity", "Time is empty")
                    editTextTime.error = "Время не может быть пустым"
                }
            }
        }
    }

    private fun selectDate() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            selectedDate = "$year-${month + 1}-$dayOfMonth"
            Log.d("EditNoteActivity", "Selected date: $selectedDate")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }
}
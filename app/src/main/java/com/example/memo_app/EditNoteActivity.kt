
package com.example.memo_app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
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
    private var selectedBackgroundResource: Int = R.drawable.background_3 // Фон по умолчанию

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        editTextNoteContent = findViewById(R.id.editTextNoteContent)
        editTextDescription = findViewById(R.id.editAddText) // Инициализация нового поля
        editTextTime = findViewById(R.id.editTextTime)
        buttonSaveNote = findViewById(R.id.buttonSaveNote)
        buttonDeleteNote = findViewById(R.id.buttonDeleteNote)
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
                editTextTime.setText(dateTimeParts[1]) // Отображение времени с двоеточием
                selectedBackgroundResource = note.backgroundColor // Установка ранее сохраненного фона
            }
        } else {
            Log.d("EditNoteActivity", "Note not found")
        }

        buttonSelectDate.setOnClickListener {
            selectDate()
        }

        // Установка фильтра на ввод для ограничения числа знаков
        editTextTime.filters = arrayOf(InputFilter.LengthFilter(5))

        editTextTime.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // Проверка формата времени при потере фокуса
                val timeText = editTextTime.text.toString().replace(":", "")
                if (timeText.length == 4) {
                    val hour = timeText.substring(0, 2).toIntOrNull()
                    val minute = timeText.substring(2, 4).toIntOrNull()
                    if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                        editTextTime.setText(String.format("%02d:%02d", hour, minute))
                    } else {
                        editTextTime.error = "Неверный формат времени"
                    }
                } else {
                    editTextTime.error = "Неверный формат времени"
                }
            }
        }

        editTextTime.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.isNotEmpty()) {
                    val cleanString = s.toString().replace(":", "")
                    val formattedString = when (cleanString.length) {
                        4 -> "${cleanString.substring(0, 2)}:${cleanString.substring(2, 4)}"
                        3 -> "${cleanString.substring(0, 1)}:${cleanString.substring(1, 3)}"
                        else -> cleanString
                    }
                    if (s.toString() != formattedString) {
                        editTextTime.setText(formattedString)
                        editTextTime.setSelection(formattedString.length)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Кнопки изменения цвета
        val buttonColor1: ImageButton = findViewById(R.id.buttonColor1)
        val buttonColor2: ImageButton = findViewById(R.id.buttonColor2)
        val buttonColor3: ImageButton = findViewById(R.id.buttonColor3)
        val buttonColor4: ImageButton = findViewById(R.id.buttonColor4)

        // Применение цвета к фону и кнопке
        buttonColor1.setOnClickListener {
            selectedBackgroundResource = R.drawable.background_1
            updateNoteItemBackground(selectedBackgroundResource)
        }
        buttonColor2.setOnClickListener {
            selectedBackgroundResource = R.drawable.background_2
            updateNoteItemBackground(selectedBackgroundResource)
        }
        buttonColor3.setOnClickListener {
            selectedBackgroundResource = R.drawable.background_3
            updateNoteItemBackground(selectedBackgroundResource)
        }
        buttonColor4.setOnClickListener {
            selectedBackgroundResource = R.drawable.background_4
            updateNoteItemBackground(selectedBackgroundResource)
        }
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
                val dateTime = "$selectedDate $time"
                note.content = updatedContent
                note.description = updatedDescription // Обновление описания
                note.dateTime = dateTime
                note.backgroundColor = selectedBackgroundResource // Обновление цвета фона
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

    private fun updateNoteItemBackground(backgroundResource: Int) {
        val inflater = LayoutInflater.from(this)
        val noteView = inflater.inflate(R.layout.note_item, null) as ViewGroup
        val deleteButton: ImageButton = noteView.findViewById(R.id.deleteButton)

        noteView.setBackgroundResource(backgroundResource)
        deleteButton.setBackgroundResource(backgroundResource)
    }
}
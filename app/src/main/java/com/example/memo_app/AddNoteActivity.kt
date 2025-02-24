package com.example.memo_app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import java.util.Calendar

class AddNoteActivity : ComponentActivity() {

    private lateinit var editTextNoteContent: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var editTextTime: EditText
    private lateinit var buttonSaveNote: ImageButton
    private lateinit var buttonSelectDate: ImageButton
    private lateinit var noteDao: NoteDao
    private var selectedDate: String = ""
    private var selectedBackgroundResource: Int = R.drawable.background_3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        editTextNoteContent = findViewById(R.id.editTextNoteContent)
        editTextDescription = findViewById(R.id.editAddText)
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

        buttonSaveNote.setOnClickListener {
            val noteContent = editTextNoteContent.text.toString()
            val noteDescription = editTextDescription.text.toString()
            val time = editTextTime.text.toString()
            Log.d("AddNoteActivity", "Note content: $noteContent, description: $noteDescription")
            if (noteContent.isNotEmpty() && selectedDate.isNotEmpty() && time.isNotEmpty()) {
                val dateTime = "$selectedDate $time"
                val note = Note(
                    id = 0, // ID будет генерироваться автоматически
                    content = noteContent,
                    description = noteDescription, // Установка описания
                    dateTime = dateTime,
                    backgroundColor = selectedBackgroundResource // Сохранение выбранного фона
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
                if (selectedDate.isEmpty()) {
                    Log.d("AddNoteActivity", "Date is empty")
                }
                if (time.isEmpty()) {
                    editTextTime.error = "Время не может быть пустым"
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

    private fun updateNoteItemBackground(backgroundResource: Int) {
        val inflater = LayoutInflater.from(this)
        val noteView = inflater.inflate(R.layout.note_item, null) as ViewGroup
        val deleteButton: ImageButton = noteView.findViewById(R.id.deleteButton)

        noteView.setBackgroundResource(backgroundResource)
        deleteButton.setBackgroundResource(backgroundResource)
    }
}
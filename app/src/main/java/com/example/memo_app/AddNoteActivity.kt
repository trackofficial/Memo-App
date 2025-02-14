package com.example.memo_app

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import java.util.Calendar

class AddNoteActivity : ComponentActivity() {

    private lateinit var editTextNoteContent: EditText
    private lateinit var editTextTime: EditText
    private lateinit var buttonSaveNote: ImageButton
    private lateinit var buttonSelectDate: Button
    private lateinit var noteDao: NoteDao
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        editTextNoteContent = findViewById(R.id.editTextNoteContent)
        editTextTime = findViewById(R.id.editTextTime)
        buttonSaveNote = findViewById(R.id.buttonSave)
        buttonSelectDate = findViewById(R.id.buttonSelectDateTime)
        noteDao = NoteDao(this)

        buttonSelectDate.setOnClickListener {
            selectDate()
        }

        buttonSaveNote.setOnClickListener {
            val noteContent = editTextNoteContent.text.toString()
            val time = editTextTime.text.toString()
            if (noteContent.isNotEmpty() && selectedDate.isNotEmpty() && time.isNotEmpty()) {
                val dateTime = "$selectedDate $time"
                val note = Note(id = 0, content = noteContent, isDeleted = false, dateTime = dateTime)
                noteDao.insert(note)
                Log.d("AddNoteActivity", "Note added: $note")
                finish()  // Закрыть активность и вернуться на главный экран
            } else {
                Log.d("AddNoteActivity", "Note content, date or time is empty")
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
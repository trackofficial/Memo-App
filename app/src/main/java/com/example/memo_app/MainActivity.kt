package com.example.memo_app

import android.Manifest
import android.content.Intent
import java.io.File
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var linearLayoutNotes: LinearLayout
    private lateinit var buttonAddNote: ImageButton
    private lateinit var buttonViewHistory: ImageButton
    private lateinit var noteDao: NoteDao
    private lateinit var notificationHelper: NotificationHelper
    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale("ru"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                notificationHelper.scheduleDailyNotification()
            } else {
                Log.e("MainActivity", "Permission for notifications not granted")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_screen)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        linearLayoutNotes = findViewById(R.id.linearLayoutNotes)
        buttonAddNote = findViewById(R.id.main_buttom)
        buttonViewHistory = findViewById(R.id.history_button)

        noteDao = NoteDao(this)
        notificationHelper = NotificationHelper(this)

        buttonAddNote.setOnClickListener {
            startActivity(Intent(this, AddNoteActivity::class.java))
        }

        buttonViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        val deletedNoteId = intent.getIntExtra("deletedNoteId", -1)
        if (deletedNoteId != -1) {
            moveNoteToHistory(deletedNoteId)
        }

        loadNotes()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationHelper.scheduleDailyNotification()
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun loadNotes() {
        linearLayoutNotes.removeAllViews()
        val notes = noteDao.getAllNotes()
        var currentDate = ""
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        notes.forEach { note ->
            try {
                val dateTime = dateTimeFormat.parse(note.dateTime)
                val noteDate = dateFormat.format(dateTime)
                val calNoteDate = Calendar.getInstance().apply {
                    time = dateTime
                }
                val dateLabel = when {
                    isSameDay(calNoteDate, today) -> "На Сегодня"
                    isSameDay(calNoteDate, tomorrow) -> "На Завтра"
                    else -> "На $noteDate"
                }
                if (dateLabel != currentDate) {
                    addDateHeaderToLayout(dateLabel)
                    currentDate = dateLabel
                }
                addNoteToLayout(note)
            } catch (e: ParseException) {
                Log.e("MainActivity", "Error parsing date: ${note.dateTime}", e)
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun addDateHeaderToLayout(date: String) {
        val inflater = LayoutInflater.from(this)
        val dateView = inflater.inflate(R.layout.date_header_item, linearLayoutNotes, false) as ViewGroup
        val dateTextView = dateView.findViewById<TextView>(R.id.dateTextView)
        dateTextView.text = date
        linearLayoutNotes.addView(dateView)
        Log.d("MainActivity", "Date header added: $date")
    }

    private fun addNoteToLayout(note: Note) {
        val inflater = LayoutInflater.from(this)
        val noteView = inflater.inflate(R.layout.note_item, linearLayoutNotes, false) as ViewGroup
        val noteTextView = noteView.findViewById<TextView>(R.id.noteTextView)
        val timeTextView = noteView.findViewById<TextView>(R.id.timeTextView)
        val noteImageView = noteView.findViewById<ImageView>(R.id.noteImageView) // Новый элемент для изображения
        val editButton = noteView.findViewById<ImageButton>(R.id.deleteButton)

        noteTextView.text = note.content
        try {
            val dateTime = dateTimeFormat.parse(note.dateTime)
            timeTextView.text = timeFormat.format(dateTime)
        } catch (e: ParseException) {
            Log.e("MainActivity", "Error parsing time: ${note.dateTime}", e)
        }

        // Установка изображения для noteView
        if (!note.imageUri.isNullOrEmpty()) {
            val imageFile = File(note.imageUri)
            if (imageFile.exists()) {
                // Если это пользовательское изображение, загружаем из файлов
                Glide.with(this)
                    .load(imageFile)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(16)))
                    .into(noteImageView)
                noteImageView.visibility = View.VISIBLE
            } else {
                // Если это имя ресурса случайного изображения, загружаем из ресурсов
                val resourceId = resources.getIdentifier(note.imageUri, "drawable", packageName)
                if (resourceId != 0) {
                    noteImageView.setImageResource(resourceId)
                    noteImageView.visibility = View.VISIBLE
                } else {
                    noteImageView.visibility = View.GONE // На случай, если ничего не найдено
                    Log.e("MainActivity", "Invalid imageUri: ${note.imageUri}")
                }
            }
        } else {
            noteImageView.visibility = View.GONE // Скрываем, если изображение отсутствует
        }

        editButton.setOnClickListener {
            Log.d("MainActivity", "Edit button clicked for note: $note")
            val intent = Intent(this, EditNoteActivity::class.java)
            intent.putExtra("noteId", note.id)
            startActivity(intent)
        }

        linearLayoutNotes.addView(noteView)
        Log.d("MainActivity", "Note added: ${note.content}")
    }

    private fun moveNoteToHistory(noteId: Int) {
        val deletedNote = noteDao.getAllNotesIncludingDeleted().firstOrNull { it.id == noteId }
        if (deletedNote != null) {
            Log.d("MainActivity", "Note moved to history: $deletedNote")
        }
    }
}
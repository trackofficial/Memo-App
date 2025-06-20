package com.example.memo_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : ComponentActivity() {

    private lateinit var linearLayoutHistory: LinearLayout
    private lateinit var noteDao: NoteDao
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private lateinit var buttonAddNote: ImageButton
    private lateinit var buttonViewHistory: ImageButton
    private lateinit var buttonViewCalendar: ImageButton
    private lateinit var focusButton: ImageButton

    private lateinit var mainButtonPlace: LinearLayout
    private lateinit var calendarButtonPlace: LinearLayout
    private lateinit var focusButtonPlace: LinearLayout
    private lateinit var historyButtonPlace: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_note)

        linearLayoutHistory = findViewById(R.id.linearLayoutNotes)
        noteDao = NoteDao(this)

        buttonAddNote = findViewById(R.id.main_button)
        buttonViewCalendar = findViewById(R.id.statistic_button)
        buttonViewHistory = findViewById(R.id.history_button)
        focusButton = findViewById(R.id.focus_button)

        mainButtonPlace = findViewById(R.id.main_button_place)
        calendarButtonPlace = findViewById(R.id.calendar_button_place)
        focusButtonPlace = findViewById(R.id.focus_button_place)
        historyButtonPlace = findViewById(R.id.history_button_place)

        mainButtonPlace.alpha = 0.5f
        calendarButtonPlace.alpha = 0.5f
        focusButtonPlace.alpha = 0.5f
        historyButtonPlace.alpha = 1f

        buttonAddNote.setOnClickListener {
            animateButtonClick(buttonAddNote)
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
        }

        buttonViewHistory.setOnClickListener {
            animateButtonClick(buttonViewHistory)
        }

        focusButton.setOnClickListener {
            animateButtonClick(focusButton)
            startActivity(Intent(this, FocusActivity::class.java))
            overridePendingTransition(0, 0)
        }

        buttonViewCalendar.setOnClickListener {
            animateButtonClick(buttonViewCalendar)
            startActivity(Intent(this, CalendarActivity::class.java))
            overridePendingTransition(0, 0)
        }

        loadAllNotes()
    }

    override fun onResume() {
        super.onResume()
        loadAllNotes()
    }

    private fun loadAllNotes() {
        linearLayoutHistory.removeAllViews()
        val allNotes = noteDao.getAllNotesIncludingDeleted()
        allNotes.forEach { note ->
            addNoteToHistoryLayout(note)
        }
    }

    private fun addNoteToHistoryLayout(note: Note) {
        val inflater = LayoutInflater.from(this)
        val noteView = inflater.inflate(R.layout.note_item_h, linearLayoutHistory, false) as ViewGroup

        if (linearLayoutHistory.findViewWithTag<View>(note.id.toString()) != null) return
        noteView.tag = note.id.toString()

        val noteTextView = noteView.findViewById<TextView>(R.id.noteTextView)
        val descriptionTextView = noteView.findViewById<TextView>(R.id.desTextView)
        val timeTextView = noteView.findViewById<TextView>(R.id.timeblock)
        val dateTextView = noteView.findViewById<TextView>(R.id.dateblock)
        val viewNoteButton = noteView.findViewById<ImageButton>(R.id.viewNoteButton)

        noteTextView.text = note.content
        descriptionTextView.text = if (!note.description.isNullOrBlank()) {
            if (note.description.length > 30) "${note.description.take(30)}..." else note.description
        } else "Нет описания"
        try {
            val parsedDate = dateTimeFormat.parse(note.dateTime)
            val cal = Calendar.getInstance().apply { time = parsedDate!! }

            val timeStr = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            val dateStr = String.format("%02d.%02d.%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))

            timeTextView.text = timeStr
            dateTextView.text = dateStr
        } catch (e: ParseException) {
            Log.e("HistoryActivity", "Ошибка парсинга даты: ${note.dateTime}", e)
            timeTextView.text = "-"
            dateTextView.text = "-"
        }

        viewNoteButton.setOnClickListener {
            val intent = Intent(this, ViewNoteActivity::class.java)
            intent.putExtra("noteId", note.id)
            startActivity(intent)
        }

        linearLayoutHistory.addView(noteView)
        noteView.alpha = 0f
        noteView.translationY = 30f
        noteView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()
    }

    private fun animateButtonClick(button: ImageButton) {
        val scaleDown = ScaleAnimation(1f, 0.9f, 1f, 0.9f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 40
            fillAfter = true
            setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    val scaleUp = ScaleAnimation(0.9f, 1f, 0.9f, 1f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
                    scaleUp.duration = 50
                    scaleUp.fillAfter = true
                    button.startAnimation(scaleUp)
                }
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })
        }
        button.startAnimation(scaleDown)
    }

    private fun updateNavigationSelection(selected: LinearLayout) {
        val all = listOf(mainButtonPlace, calendarButtonPlace, focusButtonPlace, historyButtonPlace)
        all.forEach { place ->
            val alpha = if (place == selected) 1f else 0.5f
            place.animate().alpha(alpha).setDuration(300).start()
        }
    }
}
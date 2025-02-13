package com.example.memo_app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteDao(context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun insert(note: Note) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CONTENT, note.content)
            val creationDateStr = dateFormat.format(note.creationDate)
            put(DatabaseHelper.COLUMN_CREATION_DATE, creationDateStr)
            put(DatabaseHelper.COLUMN_IS_DELETED, note.isDeleted)
        }
        db.insert(DatabaseHelper.TABLE_NAME, null, values)
        Log.d("NoteDao", "Note inserted: $note")
    }

    fun update(note: Note) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CONTENT, note.content)
            val creationDateStr = dateFormat.format(note.creationDate)
            put(DatabaseHelper.COLUMN_CREATION_DATE, creationDateStr)
            put(DatabaseHelper.COLUMN_IS_DELETED, note.isDeleted)
        }
        db.update(DatabaseHelper.TABLE_NAME, values, "${DatabaseHelper.COLUMN_ID} = ?", arrayOf(note.id.toString()))
        Log.d("NoteDao", "Note updated: $note")
    }

    fun markAsDeleted(note: Note) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_IS_DELETED, true)
        }
        db.update(DatabaseHelper.TABLE_NAME, values, "${DatabaseHelper.COLUMN_ID} = ?", arrayOf(note.id.toString()))
        Log.d("NoteDao", "Note marked as deleted: $note")
    }

    fun getAllNotes(): List<Note> {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_NAME,
            null,
            "${DatabaseHelper.COLUMN_IS_DELETED} = ?",
            arrayOf("0"),
            null,
            null,
            "${DatabaseHelper.COLUMN_CREATION_DATE} ASC"
        )

        val notes = mutableListOf<Note>()
        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val content = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTENT))
                val isDeleted = getInt(getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_DELETED)) == 1
                val creationDateStr = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATION_DATE))
                val creationDate = dateFormat.parse(creationDateStr) ?: Date()
                val note = Note(id, content, creationDate, isDeleted)
                notes.add(note)
                Log.d("NoteDao", "Note loaded: $note")
            }
            close()
        }
        return notes
    }

    fun getAllNotesIncludingDeleted(): List<Note> {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "${DatabaseHelper.COLUMN_CREATION_DATE} ASC"
        )

        val notes = mutableListOf<Note>()
        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val content = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTENT))
                val isDeleted = getInt(getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_DELETED)) == 1
                val creationDateStr = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATION_DATE))
                val creationDate = dateFormat.parse(creationDateStr) ?: Date()
                val note = Note(id, content, creationDate, isDeleted)
                notes.add(note)
                Log.d("NoteDao", "Note loaded: $note")
            }
            close()
        }
        return notes
    }
}
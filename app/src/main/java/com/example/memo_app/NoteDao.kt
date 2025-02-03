package com.example.memo_app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log

class NoteDao(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun insert(note: Note) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CONTENT, note.content)
        }
        db.insert(DatabaseHelper.TABLE_NAME, null, values)
        Log.d("NoteDao", "Note inserted: $note")
    }

    fun update(note: Note) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CONTENT, note.content)
        }
        db.update(DatabaseHelper.TABLE_NAME, values, "${DatabaseHelper.COLUMN_ID} = ?", arrayOf(note.id.toString()))
        Log.d("NoteDao", "Note updated: $note")
    }

    fun delete(note: Note) {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_NAME, "${DatabaseHelper.COLUMN_ID} = ?", arrayOf(note.id.toString()))
        Log.d("NoteDao", "Note deleted: $note")
    }

    fun getAllNotes(): List<Note> {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            null
        )

        val notes = mutableListOf<Note>()
        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val content = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTENT))
                val note = Note(id, content)
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
            null
        )

        val notes = mutableListOf<Note>()
        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val content = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTENT))
                val note = Note(id, content)
                notes.add(note)
                Log.d("NoteDao", "Note loaded: $note")
            }
            close()
        }
        return notes
    }
}
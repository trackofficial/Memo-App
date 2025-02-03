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
            put(DatabaseHelper.COLUMN_IS_DELETED, note.isDeleted)
        }
        db.insert(DatabaseHelper.TABLE_NAME, null, values)
        Log.d("NoteDao", "Note inserted: $note")
    }

    fun update(note: Note) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CONTENT, note.content)
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
            null
        )

        val notes = mutableListOf<Note>()
        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val content = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTENT))
                val isDeleted = getInt(getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_DELETED)) == 1
                val note = Note(id, content, isDeleted)
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
                val isDeleted = getInt(getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_DELETED)) == 1
                val note = Note(id, content, isDeleted)
                notes.add(note)
                Log.d("NoteDao", "Note loaded: $note")
            }
            close()
        }
        return notes
    }
}
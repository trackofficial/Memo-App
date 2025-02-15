package com.example.memo_app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NoteDao(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun insert(note: Note) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CONTENT, note.content)
            put(DatabaseHelper.COLUMN_DESCRIPTION, note.description) // Добавление описания
            put(DatabaseHelper.COLUMN_DATETIME, note.dateTime)
            put(DatabaseHelper.COLUMN_IS_DELETED, note.isDeleted)
        }
        note.id = db.insert(DatabaseHelper.TABLE_NAME, null, values).toInt()
        db.close()
    }

    fun update(note: Note) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CONTENT, note.content)
            put(DatabaseHelper.COLUMN_DESCRIPTION, note.description) // Добавление описания
            put(DatabaseHelper.COLUMN_DATETIME, note.dateTime)
            put(DatabaseHelper.COLUMN_IS_DELETED, note.isDeleted)
        }
        val selection = "${DatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(note.id.toString())
        db.update(DatabaseHelper.TABLE_NAME, values, selection, selectionArgs)
        db.close()
    }

    fun getAllNotes(): List<Note> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_NAME,
            null, // Все столбцы
            "${DatabaseHelper.COLUMN_IS_DELETED} = ?",
            arrayOf("0"),
            null,
            null,
            null
        )

        val notes = mutableListOf<Note>()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
            val content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTENT))
            val description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION)) // Получение описания
            val dateTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATETIME))
            val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_DELETED)) == 1
            val note = Note(
                id = id,
                content = content,
                description = description,
                dateTime = dateTime,
                isDeleted = isDeleted
            )
            notes.add(note)
        }
        cursor.close()
        db.close()
        return notes
    }

    fun getAllNotesIncludingDeleted(): List<Note> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_NAME,
            null, // Все столбцы
            null,
            null,
            null,
            null,
            null
        )

        val notes = mutableListOf<Note>()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
            val content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTENT))
            val description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION)) // Получение описания
            val dateTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATETIME))
            val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_DELETED)) == 1
            val note = Note(
                id = id,
                content = content,
                description = description,
                dateTime = dateTime,
                isDeleted = isDeleted
            )
            notes.add(note)
        }
        cursor.close()
        db.close()
        return notes
    }
    fun delete(note: Note) {
        val db = dbHelper.writableDatabase
        val selection = "${DatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(note.id.toString())
        db.delete(DatabaseHelper.TABLE_NAME, selection, selectionArgs)
        db.close()
    }
}
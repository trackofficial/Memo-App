package com.example.memo_app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "notes.db"
        private const val DATABASE_VERSION = 4

        const val TABLE_NAME = "notes"
        const val COLUMN_ID = "id"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_IS_DELETED = "isDeleted"
        const val COLUMN_DATETIME = "datetime" // Новое поле для даты и времени
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_NAME ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_CONTENT TEXT, "
                + "$COLUMN_IS_DELETED INTEGER DEFAULT 0, "
                + "$COLUMN_DATETIME TEXT)") // Добавление нового столбца
        db.execSQL(createTable)
        Log.d("DatabaseHelper", "Table created successfully")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_DATETIME TEXT")
        }
        Log.d("DatabaseHelper", "Table upgraded successfully")
    }
}
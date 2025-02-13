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
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_MANUAL_VALUE = "manual_value"
        const val COLUMN_IS_DELETED = "isDeleted"
        const val COLUMN_CREATION_DATE = "creation_date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_NAME ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_CONTENT TEXT, "
                + "$COLUMN_TIMESTAMP INTEGER, "
                + "$COLUMN_MANUAL_VALUE TEXT, "
                + "$COLUMN_IS_DELETED INTEGER DEFAULT 0, "
                + "$COLUMN_CREATION_DATE TEXT)") // Добавление нового столбца
        db.execSQL(createTable)
        Log.d("DatabaseHelper", "Table created successfully")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_TIMESTAMP INTEGER DEFAULT 0")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_MANUAL_VALUE TEXT DEFAULT ''")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_CREATION_DATE TEXT DEFAULT ''")
        }
        Log.d("DatabaseHelper", "Table upgraded successfully")
    }
}
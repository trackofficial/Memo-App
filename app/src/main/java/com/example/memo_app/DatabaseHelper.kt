package com.example.memo_app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    companion object {
        const val DATABASE_VERSION = 3
        const val DATABASE_NAME = "MemoApp.db"

        const val TABLE_NAME = "notes"
        const val COLUMN_ID = "id"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_DATETIME = "dateTime"
        const val COLUMN_IS_DELETED = "isDeleted"
        const val COLUMN_IMAGE_URI = "imageUri"
        const val COLUMN_GOAL = "goal"
        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE $TABLE_NAME (" +
                    "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$COLUMN_CONTENT TEXT," +
                    "$COLUMN_DESCRIPTION TEXT," +
                    "$COLUMN_DATETIME TEXT," +
                    "$COLUMN_IS_DELETED INTEGER," +
                    "$COLUMN_IMAGE_URI TEXT," +
                    "$COLUMN_GOAL TEXT)"

        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}
package com.example.memo_app

import java.util.Date

data class Note(
    var id: Int = 0,
    var content: String,
    var description: String,
    var dateTime: String,
    var isDeleted: Boolean = false,
    var backgroundColor: Int = -1 // Новый столбец для цвета фона
)
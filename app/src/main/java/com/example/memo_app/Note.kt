package com.example.memo_app

data class Note(
    val id: Int = 0,
    var content: String,
    var isDeleted: Boolean = false
)
package com.example.memo_app

data class Note(
    var id: Int = 0,
    var content: String,
    var description: String,
    var dateTime: String,
    var isDeleted: Boolean = false,
    var imageUri: String? = null
)
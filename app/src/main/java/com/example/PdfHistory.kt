package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_history")
data class PdfHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val operationType: String,
    val details: String,
    val filePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

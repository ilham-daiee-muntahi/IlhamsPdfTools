package com.example

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PdfHistory::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pdfHistoryDao(): PdfHistoryDao
}

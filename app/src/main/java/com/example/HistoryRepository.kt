package com.example

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: PdfHistoryDao) {
    val allHistory: Flow<List<PdfHistory>> = dao.getAllHistory()

    suspend fun insert(history: PdfHistory) = dao.insertHistory(history)

    suspend fun clear() = dao.clearHistory()
}

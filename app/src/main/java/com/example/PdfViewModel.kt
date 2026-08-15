package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PdfViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "pdf-tools-db"
    ).fallbackToDestructiveMigration().build()
    
    private val repository = HistoryRepository(db.pdfHistoryDao())
    
    val historyState: StateFlow<List<PdfHistory>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    fun addHistory(fileName: String, operation: String, details: String, filePath: String? = null) {
        viewModelScope.launch {
            repository.insert(PdfHistory(fileName = fileName, operationType = operation, details = details, filePath = filePath))
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            repository.clear()
        }
    }
}

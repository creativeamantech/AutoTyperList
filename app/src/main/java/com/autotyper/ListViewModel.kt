package com.autotyper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ItemRepository
    val allItems: StateFlow<List<ItemEntity>>

    init {
        val itemDao = ItemDatabase.getDatabase(application).itemDao()
        repository = ItemRepository(itemDao)
        allItems = repository.allItems.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun insert(text: String) = viewModelScope.launch {
        repository.insert(text)
    }

    fun insertMultiple(texts: List<String>) = viewModelScope.launch {
        repository.insertMultiple(texts)
    }

    fun delete(item: ItemEntity) = viewModelScope.launch {
        repository.delete(item)
    }

    fun clearAll() = viewModelScope.launch {
        repository.clearAll()
    }
}

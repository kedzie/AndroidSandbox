package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.myapplication.data.Entry
import com.example.myapplication.data.EntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: EntryRepository
) : ViewModel() {

    val pagedEntries: Flow<PagingData<Entry>> =
        repository.getPagedEntries().cachedIn(viewModelScope)

    fun addEntry(text: String) {
        if (text.isNotBlank()) {
            viewModelScope.launch {
                repository.addEntry(text)
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clear()
        }
    }
}

package com.example.myapplication.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntryRepository @Inject constructor(
    private val dao: EntryDao
) {
    fun getPagedEntries(): Flow<PagingData<Entry>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = { dao.pagingSource() }
    ).flow

    suspend fun addEntry(text: String) = dao.insert(Entry(text = text))
    suspend fun clear() = dao.clearAll()
}

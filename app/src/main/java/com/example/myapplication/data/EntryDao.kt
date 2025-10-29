package com.example.myapplication.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY id DESC")
    fun pagingSource(): PagingSource<Int, Entry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: Entry)

    @Query("DELETE FROM entries")
    suspend fun clearAll()
}

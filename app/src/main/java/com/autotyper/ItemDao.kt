package com.autotyper

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY serialNumber ASC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Insert
    suspend fun insert(item: ItemEntity)

    @Insert
    suspend fun insertAll(items: List<ItemEntity>)

    @Delete
    suspend fun delete(item: ItemEntity)

    @Query("DELETE FROM items")
    suspend fun clearAll()

    @Query("SELECT MAX(serialNumber) FROM items")
    suspend fun getMaxSerialNumber(): Int?
}

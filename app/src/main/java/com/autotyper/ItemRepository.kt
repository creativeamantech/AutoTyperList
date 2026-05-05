package com.autotyper

import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDao: ItemDao) {
    val allItems: Flow<List<ItemEntity>> = itemDao.getAllItems()

    suspend fun insert(text: String) {
        val maxSerial = itemDao.getMaxSerialNumber() ?: 0
        itemDao.insert(ItemEntity(text = text, serialNumber = maxSerial + 1))
    }

    suspend fun insertMultiple(texts: List<String>) {
        val maxSerial = itemDao.getMaxSerialNumber() ?: 0
        val items = texts.mapIndexed { index, text ->
            ItemEntity(text = text, serialNumber = maxSerial + 1 + index)
        }
        itemDao.insertAll(items)
    }

    suspend fun delete(item: ItemEntity) {
        itemDao.delete(item)
    }

    suspend fun clearAll() {
        itemDao.clearAll()
    }
}

package com.example.smartnovelreader.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reading_progress")

class ReadingProgressManager(private val context: Context) {

    companion object {
        // 使用文件路径作为键的一部分，支持多本书
        private fun getFilePositionKey(filePath: String): String = "position_${filePath.hashCode()}"
        private fun getFilePageKey(filePath: String): String = "page_${filePath.hashCode()}"

        // 创建动态的Preferences Key
        private fun createStringKey(key: String) = stringPreferencesKey(key)
        private fun createIntKey(key: String) = intPreferencesKey(key)
    }

    // 保存阅读进度 - 每本书独立保存
    suspend fun saveReadingProgress(filePath: String, scrollPosition: Int) {
        val positionKey = createIntKey(getFilePositionKey(filePath))
        val pageKey = createIntKey(getFilePageKey(filePath))

        context.dataStore.edit { preferences ->
            preferences[positionKey] = scrollPosition
            preferences[pageKey] = scrollPosition
        }

        // 同时保存最后阅读的文件，用于快速恢复
        val lastFileKey = stringPreferencesKey("last_read_file")
        context.dataStore.edit { preferences ->
            preferences[lastFileKey] = filePath
        }
    }

    // 获取特定文件的阅读进度
    suspend fun getSavedProgress(filePath: String): SavedProgress {
        val positionKey = createIntKey(getFilePositionKey(filePath))
        val pageKey = createIntKey(getFilePageKey(filePath))

        val preferences = context.dataStore.data.first()
        val scrollPosition = preferences[pageKey] ?: 0

        return SavedProgress(
            lastReadFile = filePath,
            scrollPosition = scrollPosition
        )
    }

    // 获取最后阅读的文件
    suspend fun getLastReadFile(): String {
        val lastFileKey = stringPreferencesKey("last_read_file")
        val preferences = context.dataStore.data.first()
        return preferences[lastFileKey] ?: ""
    }

    // 清除特定文件的阅读进度
    suspend fun clearProgress(filePath: String) {
        val positionKey = createIntKey(getFilePositionKey(filePath))
        val pageKey = createIntKey(getFilePageKey(filePath))

        context.dataStore.edit { preferences ->
            preferences.remove(positionKey)
            preferences.remove(pageKey)
        }
    }

    // 清除所有阅读进度
    suspend fun clearAllProgress() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

// 简化的进度数据类
data class SavedProgress(
    val lastReadFile: String,
    val scrollPosition: Int
)
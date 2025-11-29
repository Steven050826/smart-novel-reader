// ReadingProgressManager.kt
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
        // 使用文件路径+用户ID作为键，确保每个用户的阅读进度独立
        private fun getFilePositionKey(filePath: String, userId: String): String = "position_${filePath.hashCode()}_$userId"
        private fun getFilePageKey(filePath: String, userId: String): String = "page_${filePath.hashCode()}_$userId"

        // 创建动态的Preferences Key
        private fun createStringKey(key: String) = stringPreferencesKey(key)
        private fun createIntKey(key: String) = intPreferencesKey(key)
    }

    // 保存阅读进度 - 每个用户独立保存
    suspend fun saveReadingProgress(filePath: String, scrollPosition: Int, userId: String) {
        val positionKey = createIntKey(getFilePositionKey(filePath, userId))
        val pageKey = createIntKey(getFilePageKey(filePath, userId))

        context.dataStore.edit { preferences ->
            preferences[positionKey] = scrollPosition
            preferences[pageKey] = scrollPosition
        }

        // 同时保存最后阅读的文件，用于快速恢复（按用户）
        val lastFileKey = stringPreferencesKey("last_read_file_$userId")
        context.dataStore.edit { preferences ->
            preferences[lastFileKey] = filePath
        }
    }

    // 获取特定文件的阅读进度（按用户）
    suspend fun getSavedProgress(filePath: String, userId: String): SavedProgress {
        val positionKey = createIntKey(getFilePositionKey(filePath, userId))
        val pageKey = createIntKey(getFilePageKey(filePath, userId))

        val preferences = context.dataStore.data.first()
        val scrollPosition = preferences[pageKey] ?: 0

        return SavedProgress(
            lastReadFile = filePath,
            scrollPosition = scrollPosition
        )
    }

    // 获取最后阅读的文件（按用户）
    suspend fun getLastReadFile(userId: String): String {
        val lastFileKey = stringPreferencesKey("last_read_file_$userId")
        val preferences = context.dataStore.data.first()
        return preferences[lastFileKey] ?: ""
    }

    // 清除特定文件的阅读进度（按用户）
    suspend fun clearProgress(filePath: String, userId: String) {
        val positionKey = createIntKey(getFilePositionKey(filePath, userId))
        val pageKey = createIntKey(getFilePageKey(filePath, userId))

        context.dataStore.edit { preferences ->
            preferences.remove(positionKey)
            preferences.remove(pageKey)
        }
    }

    // 清除所有阅读进度（按用户）
    suspend fun clearAllProgress(userId: String) {
        // 这里需要更复杂的逻辑来清除特定用户的所有进度
        // 简单实现：清除该用户的最后阅读文件记录
        val lastFileKey = stringPreferencesKey("last_read_file_$userId")
        context.dataStore.edit { preferences ->
            preferences.remove(lastFileKey)
        }
    }
}

// 简化的进度数据类
data class SavedProgress(
    val lastReadFile: String,
    val scrollPosition: Int
)
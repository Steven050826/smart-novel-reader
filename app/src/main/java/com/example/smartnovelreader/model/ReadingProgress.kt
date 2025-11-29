package com.example.smartnovelreader.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_progress",
    primaryKeys = ["novelId", "chapterId"]
)
data class ReadingProgress(
    val novelId: String,
    val chapterId: String,
    val progress: Int = 0, // 阅读位置（字符位置或百分比）
    val readTime: Long = System.currentTimeMillis(),
    val totalProgress: Int = 0, // 总字符数
    val readDuration: Long = 0 // 阅读时长（毫秒）
) {
    // 获取阅读进度百分比
    fun getProgressPercentage(): Float {
        return if (totalProgress > 0) {
            (progress.toFloat() / totalProgress * 100).coerceIn(0f, 100f)
        } else {
            0f
        }
    }

    // 判断是否已完成阅读
    fun isCompleted(): Boolean {
        return getProgressPercentage() >= 95f
    }
}
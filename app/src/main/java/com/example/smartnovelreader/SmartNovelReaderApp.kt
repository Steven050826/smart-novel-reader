package com.example.smartnovelreader

import android.app.Application
import android.util.Log
import com.example.smartnovelreader.database.AppDatabase
import com.example.smartnovelreader.manager.ReadingProgressManager
import com.example.smartnovelreader.manager.SettingsManager
import com.example.smartnovelreader.repository.ChapterRepository
import com.example.smartnovelreader.repository.NovelRepository

class SmartNovelReaderApp : Application() {

    companion object {
        const val TAG = "SmartNovelReader"
        private lateinit var instance: SmartNovelReaderApp

        fun getInstance(): SmartNovelReaderApp = instance
    }

    // 依赖容器
    val appContainer = AppContainer()

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.d(TAG, "智能小说阅读器应用启动")
    }

    class AppContainer {
        // 数据库实例
        val database: AppDatabase by lazy {
            AppDatabase.getInstance(getInstance())
        }

        // 设置管理器
        val settingsManager: SettingsManager by lazy {
            SettingsManager(getInstance())
        }

        // 阅读进度管理器
        val readingProgressManager: ReadingProgressManager by lazy {
            ReadingProgressManager(getInstance())
        }

        // 章节仓库实例
        val chapterRepository: ChapterRepository by lazy {
            ChapterRepository(database.chapterDao())
        }

        // 小说仓库实例
        val novelRepository: NovelRepository by lazy {
            NovelRepository(
                database.novelDao(),
                chapterRepository,
                database.readingProgressDao()
            )
        }
    }
}
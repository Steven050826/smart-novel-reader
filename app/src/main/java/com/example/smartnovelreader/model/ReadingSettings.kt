package com.example.smartnovelreader.model

// 阅读设置
data class ReadingSettings(
    val fontSize: Int = 16,
    val lineSpacing: Float = 1.5f,
    val theme: ReadingTheme = ReadingTheme.LIGHT,
    val brightness: Float = 0.5f,
    val autoBrightness: Boolean = false,
    val pageTurnMode: PageTurnMode = PageTurnMode.TOUCH,
    val fontFamily: String = "system",
    val volumeKeyTurnPage: Boolean = false,
    val voiceControl: Boolean = false, // 新增语音控制
    val voiceControlLanguage: String = "zh-CN" // 语音控制语言
)

// 阅读主题
enum class ReadingTheme {
    LIGHT, DARK, SEPIA, GREEN
}

// 翻页模式
enum class PageTurnMode {
    TOUCH, // 点击
    SLIDE, // 滑动
    SCROLL, // 滚动
    SENSOR // 传感器
}
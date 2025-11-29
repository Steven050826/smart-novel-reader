package com.example.smartnovelreader.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

// 为 Context 创建扩展属性
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        // 定义设置键
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val AUTO_BRIGHTNESS_KEY = booleanPreferencesKey("auto_brightness")
        private val MANUAL_BRIGHTNESS_KEY = floatPreferencesKey("manual_brightness")
        private val VOICE_CONTROL_KEY = booleanPreferencesKey("voice_control")
        private val VOICE_CONTROL_LANGUAGE_KEY = stringPreferencesKey("voice_control_language")

        // 默认值
        private const val DEFAULT_DARK_MODE = false
        private const val DEFAULT_AUTO_BRIGHTNESS = false
        private const val DEFAULT_MANUAL_BRIGHTNESS = 0.5f
        private const val DEFAULT_VOICE_CONTROL = false
        private const val DEFAULT_VOICE_LANGUAGE = "zh-CN"
    }

    // 深色模式设置流
    val darkModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DARK_MODE_KEY] ?: DEFAULT_DARK_MODE }

    // 自动亮度设置流
    val autoBrightnessEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AUTO_BRIGHTNESS_KEY] ?: DEFAULT_AUTO_BRIGHTNESS }

    // 手动亮度设置流
    val manualBrightness: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[MANUAL_BRIGHTNESS_KEY] ?: DEFAULT_MANUAL_BRIGHTNESS }

    // 语音控制设置流
    val voiceControlEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[VOICE_CONTROL_KEY] ?: DEFAULT_VOICE_CONTROL }

    // 语音控制语言设置流
    val voiceControlLanguage: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[VOICE_CONTROL_LANGUAGE_KEY] ?: DEFAULT_VOICE_LANGUAGE }

    // 保存深色模式设置
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    // 保存自动亮度设置
    suspend fun setAutoBrightness(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BRIGHTNESS_KEY] = enabled
        }
    }

    // 保存手动亮度设置
    suspend fun setManualBrightness(brightness: Float) {
        context.dataStore.edit { preferences ->
            preferences[MANUAL_BRIGHTNESS_KEY] = brightness.coerceIn(0.1f, 1.0f)
        }
    }

    // 保存语音控制设置
    suspend fun setVoiceControl(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOICE_CONTROL_KEY] = enabled
        }
    }

    // 保存语音控制语言设置
    suspend fun setVoiceControlLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[VOICE_CONTROL_LANGUAGE_KEY] = language
        }
    }

    // 获取所有设置的快照（非 Flow）
    suspend fun getSettingsSnapshot(): SettingsSnapshot {
        val preferences = context.dataStore.data.first()
        return SettingsSnapshot(
            darkMode = preferences[DARK_MODE_KEY] ?: DEFAULT_DARK_MODE,
            autoBrightness = preferences[AUTO_BRIGHTNESS_KEY] ?: DEFAULT_AUTO_BRIGHTNESS,
            manualBrightness = preferences[MANUAL_BRIGHTNESS_KEY] ?: DEFAULT_MANUAL_BRIGHTNESS,
            voiceControl = preferences[VOICE_CONTROL_KEY] ?: DEFAULT_VOICE_CONTROL,
            voiceControlLanguage = preferences[VOICE_CONTROL_LANGUAGE_KEY] ?: DEFAULT_VOICE_LANGUAGE
        )
    }
}

// 设置快照数据类
data class SettingsSnapshot(
    val darkMode: Boolean,
    val autoBrightness: Boolean,
    val manualBrightness: Float,
    val voiceControl: Boolean,
    val voiceControlLanguage: String
)
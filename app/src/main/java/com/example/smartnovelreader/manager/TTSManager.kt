// TTSManager.kt
package com.example.smartnovelreader.manager

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.smartnovelreader.model.tts.TokenResponse
import com.example.smartnovelreader.network.tts.TTSService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class TTSManager(private val context: Context) {

    companion object {
        private const val TAG = "TTSManager"

        // Token缓存时间（提前5分钟刷新）
        private const val TOKEN_EXPIRE_BUFFER = 5 * 60 * 1000L
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentToken: String? = null
    private var tokenExpireTime: Long = 0

    // Retrofit客户端
    private val tokenClient: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(TTSService.BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val ttsClient: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(TTSService.TTS_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val tokenService: TTSService by lazy { tokenClient.create(TTSService::class.java) }
    private val ttsService: TTSService by lazy { ttsClient.create(TTSService::class.java) }

    // TTS状态监听器
    interface TTSStateListener {
        fun onTTSStart()
        fun onTTSEnd()
        fun onTTSError(error: String)
        fun onTokenRefreshed()
    }

    private var listener: TTSStateListener? = null

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 初始化TTS管理器
     */
    fun init(listener: TTSStateListener? = null) {
        this.listener = listener
        Log.d(TAG, "TTS管理器初始化完成")
    }

    /**
     * 获取Access Token
     */
    private suspend fun getAccessToken(): String? {
        return try {
            Log.d(TAG, "正在获取Access Token...")

            val response = tokenService.getAccessToken(
                grantType = "client_credentials",
                clientId = TTSService.CLIENT_ID,
                clientSecret = TTSService.CLIENT_SECRET
            )

            if (response.isSuccessful) {
                val tokenResponse = response.body()
                if (tokenResponse?.accessToken != null) {
                    currentToken = tokenResponse.accessToken
                    tokenExpireTime = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000L) - TOKEN_EXPIRE_BUFFER

                    Log.d(TAG, "Access Token获取成功: ${currentToken?.take(10)}...")
                    listener?.onTokenRefreshed()
                    currentToken
                } else {
                    val error = tokenResponse?.error ?: "未知错误"
                    val errorDesc = tokenResponse?.errorDescription ?: "无详细描述"
                    Log.e(TAG, "Token获取失败: $error - $errorDesc")
                    null
                }
            } else {
                Log.e(TAG, "Token请求失败: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取Access Token异常", e)
            null
        }
    }

    /**
     * 检查Token是否有效
     */
    private suspend fun ensureValidToken(): String? {
        val now = System.currentTimeMillis()
        return if (currentToken == null || now >= tokenExpireTime) {
            Log.d(TAG, "Token已过期或不存在，重新获取...")
            getAccessToken()
        } else {
            currentToken
        }
    }

    /**
     * 文本转语音
     */
    suspend fun textToSpeech(text: String): Boolean {
        return try {
            Log.d(TAG, "开始TTS转换，文本长度: ${text.length}")

            // 确保有有效的Token
            val token = ensureValidToken()
            if (token == null) {
                Log.e(TAG, "无法获取有效的Access Token")
                listener?.onTTSError("无法获取语音服务授权")
                return false
            }

            // 生成设备ID
            val deviceId = generateDeviceId()

            // 调用TTS API
            val response = ttsService.textToSpeech(
                text = text,
                token = token,
                deviceId = deviceId,
                speed = 5,
                pitch = 5,
                volume = 5,
                person = 0,
                audioFormat = 3
            )

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    Log.d(TAG, "TTS API调用成功，开始处理音频数据")
                    handleAudioResponse(responseBody, text)
                    true
                } else {
                    Log.e(TAG, "TTS响应体为空")
                    listener?.onTTSError("语音合成失败：响应为空")
                    false
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "TTS API调用失败: ${response.code()} - ${response.message()} - $errorBody")
                listener?.onTTSError("语音合成失败：${response.code()} - ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS转换异常", e)
            listener?.onTTSError("语音合成异常：${e.message}")
            false
        }
    }

    /**
     * 处理音频响应
     */
    private suspend fun handleAudioResponse(responseBody: ResponseBody, originalText: String) {
        return withContext(Dispatchers.IO) {
            try {
                // 创建临时音频文件
                val tempFile = File.createTempFile("tts_${System.currentTimeMillis()}", ".mp3", context.cacheDir)
                FileOutputStream(tempFile).use { outputStream ->
                    responseBody.byteStream().copyTo(outputStream)
                }

                Log.d(TAG, "音频文件保存成功: ${tempFile.absolutePath}, 大小: ${tempFile.length()} bytes")

                // 在主线程播放音频
                withContext(Dispatchers.Main) {
                    playAudioFile(tempFile, originalText)
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理音频响应失败", e)
                withContext(Dispatchers.Main) {
                    listener?.onTTSError("处理音频数据失败：${e.message}")
                }
            }
        }
    }

    /**
     * 播放音频文件
     */
    private fun playAudioFile(audioFile: File, originalText: String) {
        try {
            // 停止之前的播放
            stopTTS()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setOnPreparedListener {
                    Log.d(TAG, "开始播放TTS音频")
                    start()
                    listener?.onTTSStart()
                }
                setOnCompletionListener {
                    Log.d(TAG, "TTS播放完成")
                    listener?.onTTSEnd()
                    cleanupMediaPlayer()
                    // 删除临时文件
                    audioFile.delete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "播放TTS音频失败: what=$what, extra=$extra")
                    listener?.onTTSError("播放失败：$what")
                    cleanupMediaPlayer()
                    audioFile.delete()
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放TTS音频异常", e)
            listener?.onTTSError("播放异常：${e.message}")
            cleanupMediaPlayer()
            audioFile.delete()
        }
    }

    /**
     * 生成设备ID
     */
    private fun generateDeviceId(): String {
        // 使用Android ID或生成随机UUID作为设备ID
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        return androidId.ifEmpty { UUID.randomUUID().toString() }
    }

    /**
     * 停止TTS播放
     */
    fun stopTTS() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            mediaPlayer = null
            Log.d(TAG, "TTS已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止TTS失败", e)
        }
    }

    /**
     * 清理MediaPlayer资源
     */
    private fun cleanupMediaPlayer() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "清理MediaPlayer失败", e)
        }
    }

    /**
     * 获取当前页面文本的前50个字符
     */
    fun getCurrentPageTextPreview(pages: List<String>, currentPage: Int): String {
        return if (pages.isNotEmpty() && currentPage in pages.indices) {
            val pageText = pages[currentPage]
            // 取前50个字符，如果不足则取全部
            pageText.take(50).trim()
        } else {
            "当前页面没有内容"
        }
    }

    /**
     * 检查是否正在播放
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    /**
     * 销毁资源
     */
    fun destroy() {
        stopTTS()
        listener = null
        Log.d(TAG, "TTS管理器已销毁")
    }
}
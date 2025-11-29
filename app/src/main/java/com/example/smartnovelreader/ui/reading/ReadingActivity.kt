package com.example.smartnovelreader.ui.reading

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartnovelreader.databinding.DialogChapterListBinding
import com.example.smartnovelreader.databinding.ActivityReadingBinding
import com.example.smartnovelreader.manager.LightSensorManager
import com.example.smartnovelreader.manager.SettingsManager
import com.example.smartnovelreader.manager.VoiceControlManager
import com.example.smartnovelreader.manager.TTSManager
import com.example.smartnovelreader.model.ChapterInfo
import com.example.smartnovelreader.model.ReadingSettings
import com.example.smartnovelreader.util.ToastUtil
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import android.os.Handler
import android.os.Looper
import com.example.smartnovelreader.manager.UserManager

class ReadingActivity : AppCompatActivity(), VoiceControlManager.VoiceControlListener {
    private var currentUserId: String = ""
    private lateinit var binding: ActivityReadingBinding
    private lateinit var voiceControlManager: VoiceControlManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var ttsManager: TTSManager
    private var readingSettings = ReadingSettings()
    private var currentFilePath: String = ""
    private var currentNovelTitle: String = ""
    private var currentPosition: Int = 0
    private var currentPage: Int = 0
    private var totalPages: Int = 0
    private var novelContent: String = ""
    private var pages: List<String> = emptyList()
    private var chapters: List<ChapterInfo> = emptyList()
    private lateinit var lightSensorManager: LightSensorManager
    private var isAutoBrightnessEnabled = false
    private var isTTSEnabled = false

    // 权限请求Launcher
    private val requestVoicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showToast("录音权限已获取，语音控制已启动")
            lifecycleScope.launch {
                settingsManager.setVoiceControl(true)
            }
        } else {
            showToast("语音控制需要录音权限")
            lifecycleScope.launch {
                settingsManager.setVoiceControl(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ReadingActivity", "onCreate开始")
        try {
            binding = ActivityReadingBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // 获取当前用户ID
            val userManager = UserManager(this)
            currentUserId = userManager.getCurrentUser() ?: "default"
            Log.d("ReadingActivity", "当前用户ID: $currentUserId")

            // 获取传递的文件路径和小说标题
            currentFilePath = intent.getStringExtra("file_path") ?: ""
            currentNovelTitle = intent.getStringExtra("novel_title") ?: "未知小说"
            Log.d("ReadingActivity", "接收到的参数 - 文件路径: $currentFilePath, 标题: $currentNovelTitle, 用户: $currentUserId")

            // 初始化设置管理器
            settingsManager = SettingsManager(this)

            // 初始化界面
            setupUI()

            // 初始化语音控制
            initVoiceControl()

            // 初始化TTS
            initTTS()

            // 观察设置变化
            observeSettings()

            // 初始化光线传感器
            initLightSensor()

            // 加载小说内容
            loadNovelContent()

            Log.d("ReadingActivity", "onCreate完成")
        } catch (e: Exception) {
            Log.e("ReadingActivity", "onCreate发生错误", e)
            showToast("初始化阅读界面失败: ${e.message}")
            finish()
        }
    }

    private fun setupUI() {
        try {
            // 设置工具栏
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = currentNovelTitle

            // 设置返回按钮点击事件
            binding.toolbar.setNavigationOnClickListener {
                finish()
            }

            // 设置按钮点击事件
            binding.btnPrevious.setOnClickListener { previousPage() }
            binding.btnNext.setOnClickListener { nextPage() }
            binding.btnMenu.setOnClickListener { showChapterList() }
            binding.btnTts.setOnClickListener { toggleTTS() }

            // 添加语音控制状态显示
            updateVoiceControlStatus()
        } catch (e: Exception) {
            Log.e("ReadingActivity", "setupUI发生错误", e)
        }
    }

    /**
     * 初始化TTS
     */
    private fun initTTS() {
        try {
            ttsManager = TTSManager(this)
            ttsManager.init(object : TTSManager.TTSStateListener {
                override fun onTTSStart() {
                    showToast("开始朗读")
                    updateTTSButtonState(true)
                }

                override fun onTTSEnd() {
                    showToast("朗读结束")
                    updateTTSButtonState(false)
                }

                override fun onTTSError(error: String) {
                    showToast("语音合成失败: $error")
                    updateTTSButtonState(false)
                    Log.e("ReadingActivity", "TTS错误: $error")
                }

                override fun onTokenRefreshed() {
                    Log.d("ReadingActivity", "TTS Token刷新成功")
                }
            })

            Log.d("ReadingActivity", "TTS管理器初始化完成")
        } catch (e: Exception) {
            Log.e("ReadingActivity", "初始化TTS失败", e)
            showToast("语音功能初始化失败")
        }
    }

    /**
     * 切换TTS状态
     */
    private fun toggleTTS() {
        if (isTTSEnabled) {
            stopTTS()
        } else {
            startTTS()
        }
    }

    /**
     * 开始TTS朗读
     */
    private fun startTTS() {
        if (pages.isEmpty()) {
            showToast("当前没有可朗读的内容")
            return
        }

        val textToSpeak = ttsManager.getCurrentPageTextPreview(pages, currentPage)
        if (textToSpeak.isBlank() || textToSpeak == "当前页面没有内容") {
            showToast("当前页面没有可朗读的内容")
            return
        }

        showToast("正在合成语音...")
        lifecycleScope.launch {
            val success = ttsManager.textToSpeech(textToSpeak)
            if (success) {
                isTTSEnabled = true
            } else {
                isTTSEnabled = false
                updateTTSButtonState(false)
            }
        }
    }

    /**
     * 停止TTS朗读
     */
    private fun stopTTS() {
        ttsManager.stopTTS()
        isTTSEnabled = false
        updateTTSButtonState(false)
        showToast("朗读已停止")
    }

    /**
     * 更新TTS按钮状态
     */
    private fun updateTTSButtonState(isPlaying: Boolean) {
        binding.btnTts.text = if (isPlaying) "停止朗读" else "朗读"

        // 改变按钮颜色来指示状态
        val color = if (isPlaying) {
            ContextCompat.getColor(this, android.R.color.holo_red_light)
        } else {
            ContextCompat.getColor(this, android.R.color.holo_blue_light)
        }
        binding.btnTts.setBackgroundColor(color)
    }

    private fun loadNovelContent() {
        Log.d("ReadingActivity", "开始加载小说内容")
        if (currentFilePath.isEmpty()) {
            binding.chapterContent.text = "未找到小说文件路径"
            showToast("文件路径为空")
            return
        }

        try {
            binding.loadingProgress.visibility = View.VISIBLE
            binding.chapterContent.text = "正在加载..."

            val file = File(currentFilePath)
            Log.d("ReadingActivity", "检查文件: ${file.absolutePath}, 存在: ${file.exists()}")

            if (file.exists()) {
                // 在新线程中读取大文件，避免阻塞UI
                Thread {
                    try {
                        // 使用GBK编码读取中文TXT文件
                        novelContent = readFileWithGBK(file)
                        Log.d("ReadingActivity", "文件读取成功，长度: ${novelContent.length}")

                        // 如果内容为空，尝试用UTF-8读取
                        if (novelContent.isEmpty()) {
                            novelContent = file.readText(Charset.forName("UTF-8"))
                            Log.d("ReadingActivity", "使用UTF-8重新读取，长度: ${novelContent.length}")
                        }

                        // 分页处理
                        pages = splitIntoPages(novelContent)
                        totalPages = pages.size

                        // 提取章节信息
                        chapters = extractChapters(novelContent, pages)
                        Log.d("ReadingActivity", "提取到 ${chapters.size} 个章节")

                        runOnUiThread {
                            binding.loadingProgress.visibility = View.GONE
                            if (novelContent.isNotEmpty()) {
                                // 恢复阅读进度
                                restoreReadingProgress()
                                showToast("小说加载成功，共 $totalPages 页，${chapters.size} 章")
                            } else {
                                binding.chapterContent.text = "文件内容为空或编码不支持"
                                showToast("文件内容为空或编码不支持")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ReadingActivity", "读取文件失败", e)
                        runOnUiThread {
                            binding.loadingProgress.visibility = View.GONE
                            binding.chapterContent.text = "读取文件失败: ${e.message}"
                            showToast("读取文件失败: ${e.message}")
                        }
                    }
                }.start()
            } else {
                binding.loadingProgress.visibility = View.GONE
                binding.chapterContent.text = "文件不存在，请重新下载"
                showToast("文件不存在: $currentFilePath")
            }
        } catch (e: Exception) {
            Log.e("ReadingActivity", "加载小说内容失败", e)
            binding.loadingProgress.visibility = View.GONE
            binding.chapterContent.text = "加载失败: ${e.message}"
            showToast("加载失败: ${e.message}")
        }
    }

    /**
     * 使用GBK编码读取文件
     */
    private fun readFileWithGBK(file: File): String {
        return try {
            // 方法1: 使用GBK编码
            FileInputStream(file).use { inputStream ->
                val bytes = ByteArray(file.length().toInt())
                inputStream.read(bytes)
                String(bytes, Charset.forName("GBK"))
            }
        } catch (e: Exception) {
            Log.e("ReadingActivity", "GBK读取失败，尝试其他编码", e)
            try {
                // 方法2: 尝试GB2312
                FileInputStream(file).use { inputStream ->
                    val bytes = ByteArray(file.length().toInt())
                    inputStream.read(bytes)
                    String(bytes, Charset.forName("GB2312"))
                }
            } catch (e2: Exception) {
                Log.e("ReadingActivity", "GB2312读取失败", e2)
                try {
                    // 方法3: 尝试UTF-8
                    file.readText(Charset.forName("UTF-8"))
                } catch (e3: Exception) {
                    Log.e("ReadingActivity", "所有编码尝试都失败", e3)
                    ""
                }
            }
        }
    }

    private fun splitIntoPages(content: String): List<String> {
        if (content.isEmpty()) return emptyList()

        val pages = mutableListOf<String>()
        val charsPerPage = 1500 // 每页大约1500字符
        var start = 0

        while (start < content.length) {
            var end = start + charsPerPage
            if (end >= content.length) {
                end = content.length
            } else {
                // 尝试在段落边界处分割，避免在句子中间分页
                // 查找最近的换行符
                val nextNewline = content.indexOf('\n', end - 100)
                if (nextNewline != -1 && nextNewline > start) {
                    end = nextNewline + 1
                } else {
                    // 查找最近的句号
                    val nextPeriod = content.indexOf('。', end - 50)
                    if (nextPeriod != -1 && nextPeriod > start) {
                        end = nextPeriod + 1
                    }
                }
            }

            val pageContent = content.substring(start, end).trim()
            if (pageContent.isNotEmpty()) {
                pages.add(pageContent)
            }
            start = end
        }

        return pages
    }

    /**
     * 从小说内容中提取章节信息
     */
    private fun extractChapters(content: String, pages: List<String>): List<ChapterInfo> {
        val chapters = mutableListOf<ChapterInfo>()

        // 常见的章节标题模式
        val chapterPatterns = listOf(
            Regex("第[零一二三四五六七八九十百千0-9]+章\\s*[^\\n]*"),
            Regex("第[零一二三四五六七八九十百千0-9]+节\\s*[^\\n]*"),
            Regex("第[零一二三四五六七八九十百千0-9]+回\\s*[^\\n]*"),
            Regex("[零一二三四五六七八九十百千0-9]+、\\s*[^\\n]*")
        )

        // 查找所有章节标题
        for (pattern in chapterPatterns) {
            val matches = pattern.findAll(content)
            for (match in matches) {
                val chapterTitle = match.value.trim()
                val position = match.range.first

                // 找到章节对应的页码
                val pageIndex = findPageForPosition(position, pages, content)
                chapters.add(ChapterInfo(chapterTitle, position, pageIndex))
            }

            // 如果找到章节，就使用这种模式
            if (chapters.isNotEmpty()) {
                break
            }
        }

        // 如果没有找到章节，创建默认章节
        if (chapters.isEmpty()) {
            chapters.add(ChapterInfo("全文", 0, 0))
        }

        return chapters.distinctBy { it.position } // 去重
    }

    /**
     * 根据位置找到对应的页码
     */
    private fun findPageForPosition(position: Int, pages: List<String>, content: String): Int {
        var currentPos = 0
        for ((index, page) in pages.withIndex()) {
            val pageLength = page.length
            if (position >= currentPos && position < currentPos + pageLength) {
                return index
            }
            currentPos += pageLength
        }
        return 0
    }

    private fun showPage(pageIndex: Int) {
        if (pages.isEmpty() || pageIndex < 0 || pageIndex >= pages.size) {
            return
        }

        currentPage = pageIndex
        binding.chapterContent.text = pages[pageIndex]
        updatePageDisplay()
        saveReadingProgress()

        // 翻页后自动滚动到顶部
        binding.scrollView.post {
            binding.scrollView.scrollTo(0, 0)
        }
    }

    private fun updatePageDisplay() {
        supportActionBar?.subtitle = "第 ${currentPage + 1} / $totalPages 页"

        // 更新按钮状态
        binding.btnPrevious.isEnabled = currentPage > 0
        binding.btnNext.isEnabled = currentPage < totalPages - 1
    }

    private fun previousPage() {
        if (currentPage > 0) {
            showPage(currentPage - 1)
        }
    }

    private fun nextPage() {
        if (currentPage < totalPages - 1) {
            showPage(currentPage + 1)
        }
    }

    private fun saveReadingProgress() {
        try {
            lifecycleScope.launch {
                val readingProgressManager = (application as com.example.smartnovelreader.SmartNovelReaderApp)
                    .appContainer.readingProgressManager

                // 保存阅读进度，传入用户ID
                readingProgressManager.saveReadingProgress(currentFilePath, currentPage, currentUserId)
                Log.d("ReadingActivity", "保存阅读进度: 用户 $currentUserId, 文件 $currentFilePath, 页码 $currentPage")
            }
        } catch (e: Exception) {
            Log.e("ReadingActivity", "保存阅读进度失败", e)
        }
    }

    /**
     * 根据页码找到对应的章节
     */
    private fun findChapterForPage(pageIndex: Int): String {
        if (chapters.isEmpty()) return "全文"

        // 找到最后一个起始页码小于等于当前页码的章节
        var foundChapter = chapters[0]
        for (chapter in chapters) {
            if (chapter.pageIndex <= pageIndex) {
                foundChapter = chapter
            } else {
                break
            }
        }

        return foundChapter.title
    }

    private fun restoreReadingProgress() {
        lifecycleScope.launch {
            try {
                val readingProgressManager = (application as com.example.smartnovelreader.SmartNovelReaderApp)
                    .appContainer.readingProgressManager

                // 获取当前用户的保存进度
                val savedProgress = readingProgressManager.getSavedProgress(currentFilePath, currentUserId)
                Log.d("ReadingActivity", "恢复阅读进度: 用户 $currentUserId, 文件 ${savedProgress.lastReadFile}, 页码 ${savedProgress.scrollPosition}")

                if (savedProgress.scrollPosition in 0 until totalPages) {
                    showPage(savedProgress.scrollPosition)
                    Log.d("ReadingActivity", "成功恢复用户 $currentUserId 的阅读进度到第 ${savedProgress.scrollPosition} 页")
                } else {
                    showPage(0)
                    Log.d("ReadingActivity", "用户 $currentUserId 无有效进度，从第 0 页开始")
                }
            } catch (e: Exception) {
                Log.e("ReadingActivity", "恢复阅读进度失败", e)
                showPage(0)
            }
        }
    }

    /**
     * 显示章节列表对话框
     */
    private fun showChapterList() {
        if (chapters.isEmpty()) {
            showToast("未找到章节信息")
            return
        }

        val dialogBinding = DialogChapterListBinding.inflate(layoutInflater)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        // 设置适配器
        val chapterAdapter = ChapterAdapter { chapter ->
            showPage(chapter.pageIndex)
            dialog.dismiss()
            showToast("跳转到: ${chapter.getDisplayTitle()}")
        }

        dialogBinding.chapterRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ReadingActivity)
            adapter = chapterAdapter
        }

        chapterAdapter.submitList(chapters)

        // 关闭按钮
        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun initVoiceControl() {
        try {
            voiceControlManager = VoiceControlManager(this)
            voiceControlManager.init(this)
        } catch (e: Exception) {
            Log.e("ReadingActivity", "初始化语音控制失败", e)
        }
    }

    private fun observeSettings() {
        lifecycleScope.launch {
            try {
                settingsManager.voiceControlEnabled.collect { enabled ->
                    readingSettings = readingSettings.copy(voiceControl = enabled)
                    applyVoiceControlSetting()
                }
            } catch (e: Exception) {
                Log.e("ReadingActivity", "观察语音控制设置失败", e)
            }
        }

        lifecycleScope.launch {
            try {
                settingsManager.voiceControlLanguage.collect { language ->
                    readingSettings = readingSettings.copy(voiceControlLanguage = language)
                }
            } catch (e: Exception) {
                Log.e("ReadingActivity", "观察语音控制语言设置失败", e)
            }
        }

        lifecycleScope.launch {
            try {
                settingsManager.darkModeEnabled.collect { enabled ->
                    applyDarkModeSetting(enabled)
                }
            } catch (e: Exception) {
                Log.e("ReadingActivity", "观察深色模式设置失败", e)
            }
        }

        lifecycleScope.launch {
            try {
                settingsManager.autoBrightnessEnabled.collect { enabled ->
                    applyAutoBrightnessSetting(enabled)
                }
            } catch (e: Exception) {
                Log.e("ReadingActivity", "观察自动亮度设置失败", e)
            }
        }

        // 手动亮度监听
        lifecycleScope.launch {
            try {
                settingsManager.manualBrightness.collect { brightness ->
                    if (!isAutoBrightnessEnabled) {
                        applyManualBrightness(brightness)
                    }
                }
            } catch (e: Exception) {
                Log.e("ReadingActivity", "观察手动亮度设置失败", e)
            }
        }
    }

    private fun applyVoiceControlSetting() {
        try {
            if (readingSettings.voiceControl) {
                if (checkVoiceControlPermission()) {
                    startVoiceControl()
                } else {
                    showToast("语音控制需要录音权限")
                    requestVoicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            } else {
                stopVoiceControl()
            }
        } catch (e: Exception) {
            Log.e("ReadingActivity", "应用语音控制设置失败", e)
        }
    }

    private fun startVoiceControl() {
        try {
            if (checkVoiceControlPermission()) {
                voiceControlManager.startListening()
                showToast("语音控制已启动")
                updateVoiceControlStatus()
            } else {
                showToast("请先授予录音权限")
            }
        } catch (e: Exception) {
            Log.e("ReadingActivity", "启动语音控制失败", e)
        }
    }

    private fun stopVoiceControl() {
        try {
            voiceControlManager.stopListening()
            showToast("语音控制已停止")
            updateVoiceControlStatus()
        } catch (e: Exception) {
            Log.e("ReadingActivity", "停止语音控制失败", e)
        }
    }

    private fun updateVoiceControlStatus() {
        try {
            val statusText = if (voiceControlManager.isListening()) {
                "🔴 语音监听中..."
            } else if (readingSettings.voiceControl) {
                "🟢 语音控制已开启"
            } else {
                "⚪ 语音控制已关闭"
            }
            supportActionBar?.subtitle = statusText
        } catch (e: Exception) {
            Log.e("ReadingActivity", "更新语音控制状态失败", e)
        }
    }

    private fun checkVoiceControlPermission(): Boolean {
        return try {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.e("ReadingActivity", "检查语音控制权限失败", e)
            false
        }
    }

    private fun applyDarkModeSetting(enabled: Boolean) {
        // 简单的深色模式切换
        if (enabled) {
            binding.root.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black))
            binding.chapterContent.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        } else {
            binding.root.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
            binding.chapterContent.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        }
    }

    private fun applyAutoBrightnessSetting(enabled: Boolean) {
        try {
            isAutoBrightnessEnabled = enabled

            if (enabled) {
                // 开启自动亮度
                if (lightSensorManager.isLightSensorAvailable()) {
                    lightSensorManager.startListening()
                    showToast("自动亮度已开启 - 等待传感器数据...")
                    Log.d("ReadingActivity", "自动亮度已开启")

                    // 3秒后强制刷新一次
                    Handler(Looper.getMainLooper()).postDelayed({
                        lightSensorManager.forceRefresh()
                    }, 3000)
                } else {
                    showToast("设备不支持自动亮度")
                    lifecycleScope.launch {
                        settingsManager.setAutoBrightness(false)
                    }
                }
            } else {
                // 关闭自动亮度，恢复手动亮度
                lightSensorManager.stopListening()

                // 恢复手动亮度设置
                lifecycleScope.launch {
                    val snapshot = settingsManager.getSettingsSnapshot()
                    applyManualBrightness(snapshot.manualBrightness)
                }

                showToast("自动亮度已关闭")
                Log.d("ReadingActivity", "自动亮度已关闭")
            }
        } catch (e: Exception) {
            Log.e("ReadingActivity", "应用自动亮度设置失败", e)
            showToast("自动亮度设置失败: ${e.message}")
        }
    }

    // 手动亮度应用方法
    private fun applyManualBrightness(brightness: Float) {
        try {
            val window = window
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightness
            window.attributes = layoutParams
            Log.d("ReadingActivity", "手动亮度设置为: ${"%.0f".format(brightness * 100)}%")
        } catch (e: Exception) {
            Log.e("ReadingActivity", "应用手动亮度失败", e)
        }
    }

    // 初始化光线传感器
    private fun initLightSensor() {
        try {
            lightSensorManager = LightSensorManager(this)
            val isSensorAvailable = lightSensorManager.init(object : LightSensorManager.BrightnessChangeListener {
                override fun onBrightnessChanged(brightness: Float, lux: Float) {
                    Log.d("ReadingActivity", "亮度已调节: ${"%.0f".format(brightness * 100)}% (${"%.1f".format(lux)} lux)")
                }

                override fun onSensorError(message: String) {
                    Log.e("ReadingActivity", "光线传感器错误: $message")
                    showToast("光线传感器错误: $message")
                }

                // 传感器数据回调
                override fun onSensorData(lux: Float, targetBrightness: Float) {
                    Log.d("ReadingActivity", "传感器数据 - Lux: ${"%.1f".format(lux)}, 亮度: ${"%.0f".format(targetBrightness * 100)}%")

                    // 在界面上显示当前传感器状态（可选）
                    updateSensorStatus(lux, targetBrightness)
                }
            })

            if (isSensorAvailable) {
                Log.d("ReadingActivity", "光线传感器初始化成功: ${lightSensorManager.getSensorInfo()}")
            } else {
                Log.w("ReadingActivity", "光线传感器初始化失败")
                showToast("自动亮度功能不可用：光线传感器初始化失败")
            }
        } catch (e: Exception) {
            Log.e("ReadingActivity", "初始化光线传感器失败", e)
            showToast("初始化光线传感器失败: ${e.message}")
        }
    }

    private fun updateSensorStatus(lux: Float, brightness: Float) {
        // 可以在工具栏子标题显示传感器状态
        supportActionBar?.subtitle = "光线: ${"%.0f".format(lux)} lux"

        // 或者只在调试时显示
        if (lux > 0) {
            Log.d("ReadingActivity", "传感器活跃 - Lux: ${"%.1f".format(lux)}, 亮度: ${"%.0f".format(brightness * 100)}%")
        }
    }

    override fun onVoiceCommand(command: String) {
        try {
            when {
                command.contains("下一页") || command.contains("下一章") -> {
                    nextPage()
                    showToast("翻到下一页")
                }

                command.contains("上一页") || command.contains("上一章") -> {
                    previousPage()
                    showToast("翻到上一页")
                }

                command.contains("目录") -> {
                    showChapterList()
                    showToast("显示目录")
                }

                command.contains("设置") -> {
                    showSettings()
                    showToast("打开设置")
                }

                command.contains("书签") -> {
                    addBookmark()
                    showToast("添加书签")
                }

                command.contains("停止") || command.contains("关闭") -> {
                    lifecycleScope.launch {
                        settingsManager.setVoiceControl(false)
                    }
                    showToast("语音控制已停止")
                }

                else -> showToast("未识别的指令: $command")
            }

            if (readingSettings.voiceControl) {
                voiceControlManager.startListening()
            }
        } catch (e: Exception) {
            Log.e("ReadingActivity", "处理语音命令失败", e)
        }
    }

    override fun onVoiceError(error: String) {
        try {
            showToast("语音识别错误: $error")
            if (error.contains("权限")) {
                lifecycleScope.launch {
                    settingsManager.setVoiceControl(false)
                }
                showToast("语音控制因权限问题已关闭")
            }

            if (readingSettings.voiceControl) {
                voiceControlManager.startListening()
            }
        } catch (e: Exception) {
            Log.e("ReadingActivity", "处理语音错误失败", e)
        }
    }

    private fun showSettings() {
        showToast("设置功能开发中")
    }

    private fun addBookmark() {
        showToast("书签功能开发中")
    }

    private fun showToast(message: String) {
        ToastUtil.showShort(this, message)
    }

    override fun onResume() {
        super.onResume()
        try {
            if (readingSettings.voiceControl && checkVoiceControlPermission()) {
                voiceControlManager.startListening()
                updateVoiceControlStatus()
            }
        } catch (e: Exception) {
            Log.e("ReadingActivity", "onResume失败", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            voiceControlManager.stopListening()
            lightSensorManager.stopListening()
            stopTTS() // 停止TTS播放
            // 保存最后进度
            saveReadingProgress()
        } catch (e: Exception) {
            Log.e("ReadingActivity", "onPause失败", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            voiceControlManager.destroy()
            lightSensorManager.destroy()
            ttsManager.destroy() // 销毁TTS资源
        } catch (e: Exception) {
            Log.e("ReadingActivity", "onDestroy失败", e)
        }
    }
}
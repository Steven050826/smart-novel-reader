package com.example.smartnovelreader.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.smartnovelreader.databinding.FragmentSettingsBinding
import com.example.smartnovelreader.manager.SettingsManager
import com.example.smartnovelreader.manager.UserManager
import com.example.smartnovelreader.ui.login.LoginActivity
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsManager: SettingsManager

    // 权限请求Launcher
    private val requestVoicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限获取成功，开启语音控制
            showToast("录音权限已获取，语音控制已开启")
            lifecycleScope.launch {
                settingsManager.setVoiceControl(true)
            }
        } else {
            // 权限被拒绝
            showToast("需要录音权限才能使用语音控制")
            lifecycleScope.launch {
                settingsManager.setVoiceControl(false)
            }
            // 如果用户之前拒绝过但未选择"不再询问"，显示解释对话框
            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                showPermissionExplanationDialog()
            }
        }
    }

    // 用于跟踪自动亮度状态
    private var isAutoBrightnessEnabled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsManager = SettingsManager(requireContext())
        setupUI()
        observeSettings()
    }

    private fun setupUI() {
        // 设置用户信息
        setupUserSection()

        // 设置开关监听
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                settingsManager.setDarkMode(isChecked)
            }
            showToast("深色模式: ${if (isChecked) "开启" else "关闭"}")
        }

        binding.switchAutoBrightness.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 检查设备是否支持光线传感器
                checkLightSensorAvailability()
            } else {
                lifecycleScope.launch {
                    settingsManager.setAutoBrightness(false)
                }
                showToast("自动亮度已关闭")
            }
        }

        binding.switchVoiceControl.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 开启语音控制时检查权限
                checkAndRequestVoicePermission()
            } else {
                // 关闭语音控制
                lifecycleScope.launch {
                    settingsManager.setVoiceControl(false)
                }
                showToast("语音控制已关闭")
            }
        }

        // 设置亮度调节条
        binding.brightnessSeekBar.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isAutoBrightnessEnabled) {
                    val brightness = progress / 100f
                    lifecycleScope.launch {
                        settingsManager.setManualBrightness(brightness)
                    }
                    binding.brightnessValue.text = "${progress}%"
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                // 开始拖动时关闭自动亮度
                if (isAutoBrightnessEnabled) {
                    binding.switchAutoBrightness.isChecked = false
                    lifecycleScope.launch {
                        settingsManager.setAutoBrightness(false)
                    }
                }
            }

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                // 不需要处理
            }
        })

        // 设置按钮点击监听
        binding.btnClearCache.setOnClickListener {
            showToast("清理缓存功能开发中")
            // 实现缓存清理逻辑
        }

        binding.btnClearHistory.setOnClickListener {
            showToast("清除搜索历史功能开发中")
            // 实现搜索历史清理逻辑
        }

        // 新增：测试光线传感器按钮
        binding.btnTestSensor.setOnClickListener {
            testLightSensor()
        }
    }

    private fun setupUserSection() {
        // 显示当前用户信息
        val currentUser = UserManager(requireContext()).getCurrentUser()
        val displayName = when (currentUser) {
            "user1" -> "用户A"
            "user2" -> "用户B"
            else -> "未登录"
        }
        binding.tvCurrentUser.text = "当前用户: $displayName"

        // 退出登录按钮
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("退出登录")
            .setMessage("确定要退出当前账号吗？")
            .setPositiveButton("确定") { _, _ ->
                logout()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // SettingsFragment.kt - 修改退出登录的逻辑
    private fun logout() {
        val userManager = UserManager(requireContext())
        val currentUser = userManager.getCurrentUser()

        // 清除用户状态
        userManager.clearUser()

        // 可以在这里清除该用户的临时数据（如果需要）
        lifecycleScope.launch {
            try {
                val readingProgressManager = (requireActivity().application as com.example.smartnovelreader.SmartNovelReaderApp)
                    .appContainer.readingProgressManager
                // 清除该用户的阅读进度缓存（可选）
                // readingProgressManager.clearAllProgress(currentUser ?: "")
            } catch (e: Exception) {
                Log.e("SettingsFragment", "清除用户数据失败", e)
            }
        }

        // 跳转到登录页面
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        requireActivity().finish()

        showToast("已退出登录")
    }

    private fun observeSettings() {
        // 观察深色模式设置变化
        lifecycleScope.launch {
            settingsManager.darkModeEnabled.collect { enabled ->
                if (binding.switchDarkMode.isChecked != enabled) {
                    binding.switchDarkMode.isChecked = enabled
                }
                applyDarkMode(enabled)
            }
        }

        // 观察自动亮度设置变化
        lifecycleScope.launch {
            settingsManager.autoBrightnessEnabled.collect { enabled ->
                isAutoBrightnessEnabled = enabled
                if (binding.switchAutoBrightness.isChecked != enabled) {
                    binding.switchAutoBrightness.isChecked = enabled
                }
                binding.brightnessSeekBar.isEnabled = !enabled
                binding.brightnessLabel.alpha = if (enabled) 0.5f else 1.0f
                binding.brightnessValue.alpha = if (enabled) 0.5f else 1.0f
                applyAutoBrightness(enabled)
            }
        }

        // 观察手动亮度设置变化
        lifecycleScope.launch {
            settingsManager.manualBrightness.collect { brightness ->
                val progress = (brightness * 100).toInt()
                if (binding.brightnessSeekBar.progress != progress) {
                    binding.brightnessSeekBar.progress = progress
                    binding.brightnessValue.text = "${progress}%"
                }
            }
        }

        // 观察语音控制设置变化
        lifecycleScope.launch {
            settingsManager.voiceControlEnabled.collect { enabled ->
                if (binding.switchVoiceControl.isChecked != enabled) {
                    binding.switchVoiceControl.isChecked = enabled
                }
                updateGlobalVoiceControlSetting(enabled)
            }
        }
    }

    private fun checkAndRequestVoicePermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                showToast("语音控制已开启")
                lifecycleScope.launch {
                    settingsManager.setVoiceControl(true)
                }
            }

            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                showPermissionExplanationDialog()
            }

            else -> {
                requestVoicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("需要录音权限")
            .setMessage("语音控制功能需要访问麦克风来识别您的语音命令，如\"下一页\"、\"上一章\"等。我们不会保存或上传您的语音数据。")
            .setPositiveButton("授予权限") { _, _ ->
                requestVoicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton("取消") { _, _ ->
                lifecycleScope.launch {
                    settingsManager.setVoiceControl(false)
                }
                showToast("语音控制需要录音权限才能使用")
            }
            .setOnCancelListener {
                lifecycleScope.launch {
                    settingsManager.setVoiceControl(false)
                }
            }
            .show()
    }

    // 检查光线传感器可用性
    private fun checkLightSensorAvailability() {
        val lightSensorManager = com.example.smartnovelreader.manager.LightSensorManager(requireContext())
        if (lightSensorManager.isLightSensorAvailable()) {
            lifecycleScope.launch {
                settingsManager.setAutoBrightness(true)
            }
            showToast("自动亮度已开启")
        } else {
            showToast("设备不支持光线传感器，自动亮度功能不可用")
            binding.switchAutoBrightness.isChecked = false
        }
    }

    /**
     * 测试光线传感器
     */
    private fun testLightSensor() {
        val lightSensorManager = com.example.smartnovelreader.manager.LightSensorManager(requireContext())
        if (lightSensorManager.isLightSensorAvailable()) {
            val sensorInfo = lightSensorManager.getSensorInfo()
            showToast("光线传感器: $sensorInfo")

            // 临时启动传感器测试
            lightSensorManager.init(object :
                com.example.smartnovelreader.manager.LightSensorManager.BrightnessChangeListener {
                override fun onBrightnessChanged(brightness: Float, lux: Float) {
                    showToast("测试 - Lux: ${"%.1f".format(lux)}, 亮度: ${"%.0f".format(brightness * 100)}%")
                }

                override fun onSensorError(message: String) {
                    showToast("传感器错误: $message")
                }

                override fun onSensorData(lux: Float, targetBrightness: Float) {
                    Log.d("SettingsFragment", "测试传感器数据: ${"%.1f".format(lux)} lux")
                }
            })

            lightSensorManager.startListening()

            // 10秒后停止测试
            Handler(Looper.getMainLooper()).postDelayed({
                lightSensorManager.stopListening()
                lightSensorManager.destroy()
            }, 10000)

        } else {
            showToast("未检测到光线传感器")
        }
    }

    private fun applyDarkMode(enabled: Boolean) {
        // TODO: 实现深色模式切换逻辑
    }

    private fun applyAutoBrightness(enabled: Boolean) {
        // TODO: 实现自动亮度逻辑
    }

    private fun updateGlobalVoiceControlSetting(enabled: Boolean) {
        (requireActivity().application as? com.example.smartnovelreader.SmartNovelReaderApp)?.let { _ ->
            // 可以在这里更新应用的全局设置
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
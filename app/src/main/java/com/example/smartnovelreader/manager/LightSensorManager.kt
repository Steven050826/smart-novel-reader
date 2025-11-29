package com.example.smartnovelreader.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.os.Looper

class LightSensorManager(private val context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "LightSensorManager"

        // 光线强度范围（单位：lux） - 根据你的传感器范围调整
        private const val MIN_LUX = 5f     // 很暗的环境
        private const val MAX_LUX = 20000f  // 很亮的环境（匹配你的传感器范围）

        // 屏幕亮度范围
        private const val MIN_BRIGHTNESS = 0.1f   // 10% 最低亮度
        private const val MAX_BRIGHTNESS = 1.0f   // 100% 最高亮度
    }

    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    private var isListening = false
    private var currentBrightness = 0.5f  // 默认50%亮度

    // 新增：调试信息
    private var lastLuxValue: Float = 0f
    private var lastAppliedBrightness: Float = 0f
    private val mainHandler = Handler(Looper.getMainLooper())

    // 修改：简化Toast显示，只在亮度有显著变化时显示
    private val debugRunnable = object : Runnable {
        override fun run() {
            if (isListening) {
                Log.d(TAG, "传感器状态 - Lux: ${"%.1f".format(lastLuxValue)}, " +
                        "目标亮度: ${"%.1f%%".format(lastAppliedBrightness * 100)}, " +
                        "监听状态: $isListening")

                // 只在调试模式下显示Toast
                if (lastLuxValue > 0) {
                    Toast.makeText(
                        context,
                        "光线: ${"%.1f".format(lastLuxValue)} lux → ${"%.0f".format(lastAppliedBrightness * 100)}%",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                mainHandler.postDelayed(this, 8000) // 改为8秒一次，减少干扰
            }
        }
    }

    // 亮度变化监听器
    interface BrightnessChangeListener {
        fun onBrightnessChanged(brightness: Float, lux: Float)
        fun onSensorError(message: String)
        fun onSensorData(lux: Float, targetBrightness: Float) // 新增：传感器数据回调
    }

    private var listener: BrightnessChangeListener? = null

    /**
     * 初始化光线传感器
     */
    fun init(listener: BrightnessChangeListener? = null): Boolean {
        this.listener = listener

        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

            // 获取光线传感器
            lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

            if (lightSensor == null) {
                Log.w(TAG, "设备不支持标准光线传感器 (TYPE_LIGHT)")
                listener?.onSensorError("设备没有光线传感器")

                // 尝试寻找替代的光线传感器
                val sensorList = sensorManager?.getSensorList(Sensor.TYPE_ALL)
                val alternativeSensors = listOf(
                    "android.sensor.light",
                    "light",
                    "ambient light",
                    "TCS3720" // 你的传感器型号
                )

                for (sensor in sensorList ?: emptyList()) {
                    if (alternativeSensors.any { sensor.name.contains(it, ignoreCase = true) }) {
                        lightSensor = sensor
                        Log.d(TAG, "找到替代光线传感器: ${sensor.name}")
                        break
                    }
                }

                if (lightSensor == null) {
                    Log.e(TAG, "未找到任何光线传感器")
                    return false
                }
            }

            Log.d(TAG, "光线传感器初始化成功: ${lightSensor?.name ?: "未知"}")
            Log.d(TAG, "传感器供应商: ${lightSensor?.vendor ?: "未知"}")
            Log.d(TAG, "传感器类型: ${lightSensor?.type ?: "未知"}")

            return true
        } catch (e: Exception) {
            Log.e(TAG, "光线传感器初始化失败", e)
            listener?.onSensorError("光线传感器初始化失败: ${e.message}")
            return false
        }
    }

    /**
     * 开始监听光线变化
     */
    fun startListening() {
        if (!isListening && lightSensor != null) {
            try {
                // 使用更快的采样率
                val success = sensorManager?.registerListener(
                    this,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_UI
                )

                if (success == true) {
                    isListening = true
                    Log.d(TAG, "开始监听光线变化 - 传感器: ${lightSensor?.name}")

                    // 启动调试信息显示
                    mainHandler.removeCallbacks(debugRunnable)
                    mainHandler.post(debugRunnable)

                    // 立即显示一次当前状态
                    showImmediateStatus()

                } else {
                    Log.e(TAG, "注册传感器监听器失败")
                    listener?.onSensorError("无法启动光线传感器")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "传感器权限被拒绝", e)
                listener?.onSensorError("没有传感器访问权限")
            } catch (e: Exception) {
                Log.e(TAG, "启动传感器监听失败", e)
                listener?.onSensorError("启动传感器失败: ${e.message}")
            }
        } else if (lightSensor == null) {
            Log.w(TAG, "无法启动监听：光线传感器不可用")
            listener?.onSensorError("光线传感器不可用")
        } else {
            Log.d(TAG, "传感器已在监听状态")
        }
    }

    /**
     * 立即显示传感器状态
     */
    private fun showImmediateStatus() {
        mainHandler.post {
            Toast.makeText(
                context,
                "自动亮度已开启 - 等待传感器数据...",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * 停止监听光线变化
     */
    fun stopListening() {
        if (isListening) {
            try {
                sensorManager?.unregisterListener(this)
                isListening = false

                // 停止调试信息
                mainHandler.removeCallbacks(debugRunnable)

                Log.d(TAG, "停止监听光线变化")

                // 显示停止状态
                mainHandler.post {
                    Toast.makeText(
                        context,
                        "自动亮度已关闭",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "停止传感器监听失败", e)
            }
        }
    }

    /**
     * 传感器数据变化回调
     */
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_LIGHT ||
                it.sensor.name.contains("light", ignoreCase = true) ||
                it.sensor.vendor.contains("ams", ignoreCase = true)) { // 你的传感器供应商

                val lux = it.values[0]
                lastLuxValue = lux

                val targetBrightness = calculateBrightness(lux)
                lastAppliedBrightness = targetBrightness

                // 应用亮度调节
                applyBrightness(targetBrightness)

                // 回调监听器
                listener?.onBrightnessChanged(targetBrightness, lux)
                listener?.onSensorData(lux, targetBrightness) // 新增回调

                Log.d(TAG, "光线强度: ${"%.1f".format(lux)} lux, " +
                        "目标亮度: ${"%.1f%%".format(targetBrightness * 100)}")
            }
        }
    }

    /**
     * 根据光线强度计算目标亮度 - 优化算法
     */
    private fun calculateBrightness(lux: Float): Float {
        // 对数值映射，更符合人眼感知
        val clampedLux = lux.coerceIn(MIN_LUX, MAX_LUX)

        // 使用对数尺度，因为人眼对光强的感知是对数的
        val logMin = kotlin.math.log10(MIN_LUX.toDouble())
        val logMax = kotlin.math.log10(MAX_LUX.toDouble())
        val logLux = kotlin.math.log10(clampedLux.toDouble())

        val normalizedLux = ((logLux - logMin) / (logMax - logMin)).toFloat()

        // 使用缓动函数使变化更平滑
        val brightness = (normalizedLux * (MAX_BRIGHTNESS - MIN_BRIGHTNESS) + MIN_BRIGHTNESS)

        return brightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
    }

    /**
     * 应用亮度调节到当前Activity - 修复版本
     */
    private fun applyBrightness(brightness: Float) {
        try {
            currentBrightness = brightness

            if (context is AppCompatActivity) {
                val window = context.window
                val layoutParams = window.attributes

                // 检查系统自动亮度设置
                val systemAutoBrightness = try {
                    Settings.System.getInt(context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE) ==
                            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                } catch (e: Exception) {
                    false
                }

                if (systemAutoBrightness) {
                    Log.w(TAG, "系统自动亮度已开启，可能覆盖应用设置")
                    // 即使系统自动亮度开启，我们也尝试设置，但效果可能有限
                }

                // 直接设置窗口亮度
                layoutParams.screenBrightness = brightness
                window.attributes = layoutParams

                Log.d(TAG, "应用亮度: ${"%.0f".format(brightness * 100)}% " +
                        "(系统自动亮度: $systemAutoBrightness)")

            }
        } catch (e: Exception) {
            Log.e(TAG, "应用亮度调节失败", e)
            listener?.onSensorError("应用亮度失败: ${e.message}")
        }
    }

    fun getCurrentBrightness(): Float = currentBrightness

    fun getLastLuxValue(): Float = lastLuxValue

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "传感器精度变化: $accuracy")
    }

    fun destroy() {
        stopListening()
        sensorManager = null
        lightSensor = null
        listener = null
        Log.d(TAG, "光线传感器管理器已销毁")
    }

    fun isLightSensorAvailable(): Boolean {
        return try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
            lightSensor != null
        } catch (e: Exception) {
            false
        }
    }

    fun getSensorInfo(): String {
        return if (lightSensor != null) {
            "传感器: ${lightSensor?.name ?: "未知"}, " +
                    "类型: ${lightSensor?.type ?: "未知"}, " +
                    "供应商: ${lightSensor?.vendor ?: "未知"}"
        } else {
            "无光线传感器"
        }
    }

    /**
     * 强制刷新当前亮度（用于调试）
     */
    fun forceRefresh() {
        if (lastLuxValue > 0) {
            val targetBrightness = calculateBrightness(lastLuxValue)
            applyBrightness(targetBrightness)
            Log.d(TAG, "强制刷新亮度: ${"%.1f".format(lastLuxValue)} lux → ${"%.0f".format(targetBrightness * 100)}%")
        }
    }
}
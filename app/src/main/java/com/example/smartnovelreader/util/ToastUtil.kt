package com.example.smartnovelreader.util

import android.content.Context
import android.widget.Toast

object ToastUtil {
    private var currentToast: Toast? = null

    /**
     * 显示短时间Toast，自动取消前一个Toast
     */
    fun showShort(context: Context, message: String) {
        currentToast?.cancel()
        currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        currentToast?.show()
    }

    /**
     * 显示长时间Toast，自动取消前一个Toast
     */
    fun showLong(context: Context, message: String) {
        currentToast?.cancel()
        currentToast = Toast.makeText(context, message, Toast.LENGTH_LONG)
        currentToast?.show()
    }

    /**
     * 取消当前显示的Toast
     */
    fun cancel() {
        currentToast?.cancel()
        currentToast = null
    }
}
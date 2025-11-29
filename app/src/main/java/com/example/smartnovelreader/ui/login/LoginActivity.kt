// LoginActivity.kt
package com.example.smartnovelreader.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnovelreader.MainActivity
import com.example.smartnovelreader.databinding.ActivityLoginBinding
import com.example.smartnovelreader.manager.UserManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnUser1.setOnClickListener {
            loginAs("user1", "用户A")
        }
        binding.btnUser2.setOnClickListener {
            loginAs("user2", "用户B")
        }
    }

    private fun loginAs(userId: String, displayName: String) {
        // 保存用户ID
        UserManager(this).login(userId)

        // 启动主页面并传递用户显示名称
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user_display_name", displayName)
        startActivity(intent)
        finish()
    }
}
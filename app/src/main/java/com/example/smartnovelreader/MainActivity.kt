// MainActivity.kt
package com.example.smartnovelreader

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnovelreader.databinding.ActivityMainBinding
import com.example.smartnovelreader.manager.UserManager
import com.example.smartnovelreader.ui.login.LoginActivity
import com.example.smartnovelreader.ui.search.SearchFragment
import com.example.smartnovelreader.ui.settings.SettingsFragment
import com.example.smartnovelreader.ui.shelf.ShelfFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查登录状态
        val userManager = UserManager(this)
        if (!userManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // 设置标题显示当前用户
        val currentUser = userManager.getCurrentUser()
        val displayName = when (currentUser) {
            "user1" -> "用户A"
            "user2" -> "用户B"
            else -> "用户"
        }
        supportActionBar?.title = "$displayName 的书架"

        setupUI(savedInstanceState)
    }

    private fun setupUI(savedInstanceState: Bundle?) {
        // 设置默认显示书架Fragment
        if (savedInstanceState == null) {
            showFragment(ShelfFragment(), "shelf")
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.shelfFragment -> {
                    showFragment(ShelfFragment(), "shelf")
                    // 更新标题
                    val userManager = UserManager(this)
                    val currentUser = userManager.getCurrentUser()
                    val displayName = when (currentUser) {
                        "user1" -> "用户A"
                        "user2" -> "用户B"
                        else -> "用户"
                    }
                    supportActionBar?.title = "$displayName 的书架"
                    true
                }
                R.id.searchFragment -> {
                    showFragment(SearchFragment(), "search")
                    supportActionBar?.title = getString(R.string.search)
                    true
                }
                R.id.settingsFragment -> {
                    showFragment(SettingsFragment(), "settings")
                    supportActionBar?.title = getString(R.string.settings)
                    true
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: androidx.fragment.app.Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit()
    }
}
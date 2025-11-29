package com.example.smartnovelreader

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnovelreader.databinding.ActivityMainBinding
import com.example.smartnovelreader.ui.search.SearchFragment
import com.example.smartnovelreader.ui.settings.SettingsFragment
import com.example.smartnovelreader.ui.shelf.ShelfFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupUI(savedInstanceState)
    }

    private fun setupUI(savedInstanceState: Bundle?) {
        // 设置默认显示书架Fragment
        if (savedInstanceState == null) {
            showFragment(ShelfFragment(), "shelf")
            supportActionBar?.title = getString(R.string.my_shelf)
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.shelfFragment -> {
                    showFragment(ShelfFragment(), "shelf")
                    supportActionBar?.title = getString(R.string.my_shelf)
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
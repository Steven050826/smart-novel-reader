// ShelfViewModel.kt
package com.example.smartnovelreader.ui.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnovelreader.model.Novel
import com.example.smartnovelreader.repository.NovelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShelfViewModel(private val novelRepository: NovelRepository) : ViewModel() {

    // 需要传入userId来获取对应书架
    fun getNovelsInShelf(userId: String) = novelRepository.getNovelsInShelf(userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addToShelf(novel: Novel) {
        viewModelScope.launch {
            novelRepository.addToShelf(novel)
        }
    }

    fun removeFromShelf(novelId: String) {
        viewModelScope.launch {
            novelRepository.removeFromShelf(novelId)
        }
    }

    fun updateLastRead(novelId: String, chapterId: String) {
        viewModelScope.launch {
            novelRepository.updateLastRead(novelId, chapterId)
        }
    }

    fun getShelfCount(userId: String): Int {
        var count = 0
        viewModelScope.launch {
            count = novelRepository.getShelfCount(userId)
        }
        return count
    }
}
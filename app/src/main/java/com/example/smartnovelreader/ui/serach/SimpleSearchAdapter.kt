package com.example.smartnovelreader.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnovelreader.databinding.ItemSearchResultBinding
import com.example.smartnovelreader.model.SimpleNovel

class SimpleSearchAdapter(
    private val onDownloadClick: (SimpleNovel) -> Unit,
    private val onItemClick: (SimpleNovel) -> Unit
) : ListAdapter<SimpleNovel, SimpleSearchAdapter.SimpleSearchViewHolder>(SimpleSearchDiffCallback) {

    class SimpleSearchViewHolder(
        private val binding: ItemSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(novel: SimpleNovel, onDownloadClick: (SimpleNovel) -> Unit, onItemClick: (SimpleNovel) -> Unit) {
            binding.novelTitle.text = novel.title
            binding.novelAuthor.text = "作者: ${novel.author ?: "未知"}"
            binding.novelDescription.text = novel.description ?: "暂无描述"
            binding.novelInfo.text = "点击下载按钮获取TXT文件"

            // 设置下载按钮
            binding.btnAddToShelf.text = "下载TXT"
            binding.btnAddToShelf.setOnClickListener {
                onDownloadClick(novel)
            }

            // 设置整个item的点击事件
            binding.root.setOnClickListener {
                onItemClick(novel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleSearchViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SimpleSearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SimpleSearchViewHolder, position: Int) {
        holder.bind(getItem(position), onDownloadClick, onItemClick)
    }
}

object SimpleSearchDiffCallback : DiffUtil.ItemCallback<SimpleNovel>() {
    override fun areItemsTheSame(oldItem: SimpleNovel, newItem: SimpleNovel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SimpleNovel, newItem: SimpleNovel): Boolean {
        return oldItem == newItem
    }
}
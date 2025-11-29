package com.example.smartnovelreader.ui.shelf

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnovelreader.databinding.ItemNovelBinding
import com.example.smartnovelreader.model.Novel

class NovelAdapter(
    private val onItemClick: (Novel) -> Unit = {},
    private val onLongClick: (Novel) -> Boolean = { false }
) : ListAdapter<Novel, NovelAdapter.NovelViewHolder>(NovelDiffCallback) {

    class NovelViewHolder(private val binding: ItemNovelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(novel: Novel, onItemClick: (Novel) -> Unit, onLongClick: (Novel) -> Boolean) {
            binding.novelTitle.text = novel.title
            binding.novelAuthor.text = "作者: ${novel.author}"

            // 显示文件来源
            binding.novelSource.text = "来源: ${novel.source}"

            // 删除进度显示
            binding.novelProgress.visibility = View.GONE
            binding.progressBar.visibility = View.GONE

            binding.root.setOnClickListener {
                onItemClick(novel)
            }

            binding.root.setOnLongClickListener {
                onLongClick(novel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NovelViewHolder {
        val binding = ItemNovelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NovelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NovelViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick, onLongClick)
    }
}

object NovelDiffCallback : DiffUtil.ItemCallback<Novel>() {
    override fun areItemsTheSame(oldItem: Novel, newItem: Novel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Novel, newItem: Novel): Boolean {
        return oldItem == newItem
    }
}
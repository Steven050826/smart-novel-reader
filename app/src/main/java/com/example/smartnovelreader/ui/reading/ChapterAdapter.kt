package com.example.smartnovelreader.ui.reading

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnovelreader.databinding.ItemChapterBinding
import com.example.smartnovelreader.model.ChapterInfo

class ChapterAdapter(
    private val onChapterClick: (ChapterInfo) -> Unit
) : ListAdapter<ChapterInfo, ChapterAdapter.ChapterViewHolder>(ChapterDiffCallback) {

    class ChapterViewHolder(
        private val binding: ItemChapterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chapter: ChapterInfo, onChapterClick: (ChapterInfo) -> Unit) {
            binding.chapterTitle.text = chapter.getDisplayTitle()
            binding.chapterPage.text = "第${chapter.pageIndex + 1}页"

            binding.root.setOnClickListener {
                onChapterClick(chapter)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val binding = ItemChapterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        holder.bind(getItem(position), onChapterClick)
    }
}

object ChapterDiffCallback : DiffUtil.ItemCallback<ChapterInfo>() {
    override fun areItemsTheSame(oldItem: ChapterInfo, newItem: ChapterInfo): Boolean {
        return oldItem.title == newItem.title && oldItem.position == newItem.position
    }

    override fun areContentsTheSame(oldItem: ChapterInfo, newItem: ChapterInfo): Boolean {
        return oldItem == newItem
    }
}
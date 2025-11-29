package com.example.smartnovelreader.ui.search

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartnovelreader.databinding.FragmentSearchBinding
import com.example.smartnovelreader.model.SimpleNovel
import com.example.smartnovelreader.network.SimpleRetrofitClient
import com.example.smartnovelreader.ui.reading.ReadingActivity
import kotlinx.coroutines.launch
import java.io.File


class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchAdapter: SimpleSearchAdapter
    private val downloadedFiles = mutableMapOf<Int, String>() // 存储下载文件路径

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        // 初始化适配器
        searchAdapter = SimpleSearchAdapter(
            onDownloadClick = { novel ->
                downloadNovel(novel)
            },
            onItemClick = { novel ->
                openNovel(novel)
            }
        )

        // 设置RecyclerView
        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }

        // 设置搜索框监听
        binding.searchView.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    performSearch(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    showInitialState()
                }
                return false
            }
        })

        showInitialState()
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            showInitialState()
            return
        }

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
            binding.noResultsText.visibility = View.GONE
            binding.searchResultsRecyclerView.visibility = View.GONE

            try {
                val response = SimpleRetrofitClient.apiService.searchNovels(query)
                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    if (searchResponse?.success == true && searchResponse.data.isNotEmpty()) {
                        showSearchResults(searchResponse.data)
                    } else {
                        showNoResults()
                    }
                } else {
                    showError("搜索失败: ${response.code()}")
                }
            } catch (e: Exception) {
                showError("网络错误: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun downloadNovel(novel: SimpleNovel) {
        try {
            val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val downloadUrl = novel.getDownloadUrl()
            val request = DownloadManager.Request(Uri.parse(downloadUrl))

            // 设置下载参数
            request.setTitle("下载: ${novel.title}")
            request.setDescription("正在下载 ${novel.title}.txt")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // 设置下载路径
            val fileName = "${novel.title}.txt"
            request.setDestinationInExternalFilesDir(
                requireContext(),
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )

            // 开始下载
            val downloadId = downloadManager.enqueue(request)

            showToast("开始下载: ${novel.title}")

            // 监听下载完成
            monitorDownload(downloadId, novel)

        } catch (e: Exception) {
            showToast("下载失败: ${e.message}")
        }
    }

    private fun monitorDownload(downloadId: Long, novel: SimpleNovel) {
        // 这里简化处理，实际应该使用BroadcastReceiver监听下载完成
        showToast("下载完成后请重新搜索并点击小说阅读")
    }

    private fun openNovel(novel: SimpleNovel) {
        val fileName = "${novel.title}.txt"
        val filePath = getDownloadedFilePath(fileName)
        val file = File(filePath)

        if (file.exists()) {
            val intent = Intent(requireContext(), ReadingActivity::class.java).apply {
                putExtra("file_path", filePath)
                putExtra("novel_title", novel.title)
            }
            startActivity(intent)
        } else {
            showToast("请先下载小说文件")
        }
    }

    // 获取下载文件路径的方法
    private fun getDownloadedFilePath(fileName: String): String {
        val downloadsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDir, fileName).absolutePath
    }

    private fun showInitialState() {
        binding.emptyStateText.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.GONE
        binding.noResultsText.visibility = View.GONE
        searchAdapter.submitList(emptyList())
    }

    private fun showSearchResults(novels: List<SimpleNovel>) {
        binding.emptyStateText.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.VISIBLE
        binding.noResultsText.visibility = View.GONE
        searchAdapter.submitList(novels)
    }

    private fun showNoResults() {
        binding.emptyStateText.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.GONE
        binding.noResultsText.visibility = View.VISIBLE
    }

    private fun showError(errorMessage: String) {
        binding.emptyStateText.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.GONE
        binding.noResultsText.visibility = View.VISIBLE
        binding.noResultsText.text = errorMessage
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
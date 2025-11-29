package com.example.smartnovelreader.ui.shelf

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartnovelreader.databinding.FragmentShelfBinding
import com.example.smartnovelreader.manager.UserManager
import com.example.smartnovelreader.model.Novel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class ShelfFragment : Fragment() {

    private var _binding: FragmentShelfBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShelfViewModel by viewModels {
        ShelfViewModelFactory((requireActivity().application as com.example.smartnovelreader.SmartNovelReaderApp).appContainer.novelRepository)
    }

    private lateinit var novelAdapter: NovelAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShelfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeData()
    }

    private fun setupUI() {
        // 显示当前用户
        val currentUser = UserManager(requireContext()).getCurrentUser()
        //binding.toolbar.title = "${currentUser ?: "用户"}的书架"

        // 初始化适配器
        novelAdapter = NovelAdapter(
            onItemClick = { novel ->
                openNovel(novel)
            },
            onLongClick = { novel ->
                showNovelOptions(novel)
                true
            }
        )

        // 设置RecyclerView
        binding.novelRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = novelAdapter
        }

        // 设置 FAB 点击事件 - 改为添加本地文件
        binding.fabAdd.setOnClickListener {
            showAddBookDialog()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            val currentUser = UserManager(requireContext()).getCurrentUser() ?: "default"

            // 获取当前用户的独立书架
            viewModel.getNovelsInShelf(currentUser).collectLatest { novels ->
                Log.d("ShelfFragment", "用户${currentUser}的书架数据更新: ${novels.size} 本小说")

                if (novels.isNotEmpty()) {
                    showNovelList()
                    novelAdapter.submitList(novels)
                    showToast("${getUserDisplayName(currentUser)}的书架 - ${novels.size}本书")
                } else {
                    showEmptyState()
                }
            }
        }
    }
    private fun getUserDisplayName(userId: String): String {
        return when (userId) {
            "user1" -> "用户A"
            "user2" -> "用户B"
            else -> "用户"
        }
    }

    private fun showNovelList() {
        binding.emptyText.visibility = View.GONE
        binding.novelRecyclerView.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        binding.emptyText.visibility = View.VISIBLE
        binding.novelRecyclerView.visibility = View.GONE
    }

    // ShelfFragment.kt - 修改打开小说的逻辑
    private fun openNovel(novel: Novel) {
        Log.d("ShelfFragment", "尝试打开小说: ${novel.title}")

        try {
            // 检查文件是否存在
            val filePath = novel.coverUrl // 这里我们复用coverUrl字段存储文件路径
            Log.d("ShelfFragment", "文件路径: $filePath")

            if (filePath == null) {
                showToast("小说文件路径为空")
                return
            }

            val file = File(filePath)
            Log.d("ShelfFragment", "文件存在: ${file.exists()}, 文件大小: ${file.length()}")

            if (file.exists()) {
                val intent = android.content.Intent(requireContext(), com.example.smartnovelreader.ui.reading.ReadingActivity::class.java).apply {
                    putExtra("file_path", filePath)
                    putExtra("novel_title", novel.title)
                    // 注意：这里不需要传递用户ID，因为ReadingActivity会自己获取
                }
                Log.d("ShelfFragment", "启动ReadingActivity，用户: ${UserManager(requireContext()).getCurrentUser()}")
                startActivity(intent)
            } else {
                showToast("小说文件不存在，可能已被删除")
            }
        } catch (e: Exception) {
            Log.e("ShelfFragment", "打开小说时发生错误", e)
            showToast("打开小说失败: ${e.message}")
        }
    }

    private fun showNovelOptions(novel: Novel) {
        val options = arrayOf("查看详情", "移出书架", "取消")

        AlertDialog.Builder(requireContext())
            .setTitle(novel.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showNovelDetails(novel)
                    1 -> removeFromShelf(novel)
                    // 2 是取消，不需要处理
                }
            }
            .show()
    }

    private fun showNovelDetails(novel: Novel) {
        val currentUser = UserManager(requireContext()).getCurrentUser()
        val details = """
            标题: ${novel.title}
            作者: ${novel.author}
            分类: ${novel.category}
            状态: ${novel.status}
            字数: ${novel.wordCount}
            总章节: ${novel.totalChapters}
            文件路径: ${novel.coverUrl ?: "未知"}
            当前用户: $currentUser
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("小说详情")
            .setMessage(details)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun removeFromShelf(novel: Novel) {
        AlertDialog.Builder(requireContext())
            .setTitle("移出书架")
            .setMessage("确定要将《${novel.title}》移出书架吗？")
            .setPositiveButton("确定") { _, _ ->
                viewModel.removeFromShelf(novel.id)
                showToast("已移出书架")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddBookDialog() {
        val options = arrayOf("从下载目录添加", "添加示例小说", "取消")

        AlertDialog.Builder(requireContext())
            .setTitle("添加小说")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> browseDownloadDirectory()
                    1 -> addSampleNovel()
                    // 2 是取消，不需要处理
                }
            }
            .show()
    }

    private fun browseDownloadDirectory() {
        val downloadDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (downloadDir == null || !downloadDir.exists()) {
            showToast("下载目录不存在")
            return
        }

        val txtFiles = downloadDir.listFiles { file ->
            file.isFile && file.name.endsWith(".txt", ignoreCase = true)
        }

        if (txtFiles.isNullOrEmpty()) {
            showToast("下载目录中没有找到TXT文件")
            return
        }

        val fileNames = txtFiles.map { it.name }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("选择TXT文件")
            .setItems(fileNames) { _, which ->
                val selectedFile = txtFiles[which]
                processTxtFile(selectedFile)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun processTxtFile(file: File) {
        try {
            Log.d("ShelfFragment", "处理TXT文件: ${file.absolutePath}")

            // 解析文件信息
            val novelInfo = parseTxtFile(file)
            val currentUser = UserManager(requireContext()).getCurrentUser() ?: "default"

            // 创建小说对象
            val novel = Novel(
                id = "local_${file.nameWithoutExtension}_${System.currentTimeMillis()}",
                title = novelInfo.title,
                author = novelInfo.author,
                coverUrl = file.absolutePath,
                description = novelInfo.description,
                category = "本地导入",
                status = "已完结",
                source = "本地文件",
                totalChapters = 1,
                wordCount = file.length(),
                lastReadTime = System.currentTimeMillis(),
                isInShelf = true,
                userId = currentUser // 设置用户ID
            )

            Log.d("ShelfFragment", "为用户${currentUser}创建小说对象: ${novel.title}")

            viewModel.addToShelf(novel)
            showToast("《${novelInfo.title}》已添加到${getUserDisplayName(currentUser)}的书架")

        } catch (e: Exception) {
            Log.e("ShelfFragment", "处理TXT文件失败", e)
            showToast("文件解析失败: ${e.message}")
        }
    }

    private fun parseTxtFile(file: File): NovelInfo {
        // 从文件名提取标题
        val title = file.nameWithoutExtension

        // 读取文件前几行尝试提取作者和描述
        var author = "未知作者"
        var description = "暂无描述"

        try {
            file.bufferedReader().use { reader ->
                var lineCount = 0
                val firstLines = StringBuilder()

                // 修复：使用明确的循环条件，避免变量初始化问题
                while (lineCount < 20) {
                    val line = reader.readLine()
                    if (line == null) break

                    firstLines.append(line).append("\n")

                    // 尝试从开头几行提取作者信息
                    if (line.contains("作者") || line.contains("著") || line.contains("写")) {
                        val authorMatch = Regex("""(作者|著)[:：]\s*([^\s，。]+)""").find(line)
                        if (authorMatch != null && authorMatch.groupValues.size > 2) {
                            author = authorMatch.groupValues[2]
                        }
                    }

                    lineCount++
                }

                // 使用前200个字符作为描述
                description = firstLines.toString().take(200)
                if (description.length >= 200) {
                    description += "..."
                }
            }
        } catch (e: Exception) {
            // 如果读取失败，使用默认值
            description = "文件大小: ${formatFileSize(file.length())}"
            Log.w("ShelfFragment", "解析TXT文件描述失败，使用默认描述", e)
        }

        return NovelInfo(title, author, description)
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    private fun addSampleNovel() {
        val currentUser = UserManager(requireContext()).getCurrentUser() ?: "default"
        val (bookTitle, authorName) = when (currentUser) {
            "user1" -> Pair("用户A的书籍1", "作者A")
            "user2" -> Pair("用户B的书籍1", "作者B")
            else -> Pair("示例书籍", "未知作者")
        }

        val sampleNovel = Novel(
            id = "novel_${currentUser}_${System.currentTimeMillis()}",
            title = bookTitle,
            author = authorName,
            description = "这是${getUserDisplayName(currentUser)}的专属书籍",
            category = "专属",
            status = "连载中",
            source = "本地",
            totalChapters = 100,
            wordCount = 500000L,
            lastReadTime = System.currentTimeMillis(),
            isInShelf = true,
            userId = currentUser // 设置用户ID
        )

        viewModel.addToShelf(sampleNovel)
        showToast("已为${getUserDisplayName(currentUser)}添加专属书籍")
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// 小说信息数据类
data class NovelInfo(
    val title: String,
    val author: String,
    val description: String
)

// ViewModel Factory
class ShelfViewModelFactory(private val novelRepository: com.example.smartnovelreader.repository.NovelRepository) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShelfViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShelfViewModel(novelRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
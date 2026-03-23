package moe.ouom.neriplayer.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.io.path.extension

private data class DesktopTrack(
    val file: File,
    val title: String = file.nameWithoutExtension,
    val subtitle: String = file.parentFile?.name ?: "未分类",
)

private val supportedAudioExtensions = setOf("mp3", "flac", "wav", "ogg", "m4a", "aac")

fun main() = application {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    Window(
        onCloseRequest = ::exitApplication,
        title = "NeriPlayer Desktop",
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                DesktopApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DesktopApp() {
    val tracks = remember { mutableStateListOf<DesktopTrack>() }
    var query by remember { mutableStateOf("") }
    var selectedTrack by remember { mutableStateOf<DesktopTrack?>(null) }
    var currentFolder by remember { mutableStateOf<File?>(null) }
    val queue = remember { mutableStateListOf<DesktopTrack>() }
    val filteredTracks = remember(tracks, query) {
        tracks.filter {
            query.isBlank() ||
                it.title.contains(query, ignoreCase = true) ||
                it.subtitle.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F1115),
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F1115)),
        ) {
            NavigationRailCard(
                modifier = Modifier.fillMaxHeight().width(280.dp),
                currentFolder = currentFolder,
                trackCount = tracks.size,
                queueSize = queue.size,
                onPickFolder = {
                    chooseFolder()?.let { folder ->
                        currentFolder = folder
                        tracks.clear()
                        tracks.addAll(scanAudioFiles(folder))
                        queue.clear()
                        selectedTrack = tracks.firstOrNull()
                    }
                },
                onImportFiles = {
                    chooseFiles().takeIf { it.isNotEmpty() }?.let { files ->
                        currentFolder = null
                        tracks.clear()
                        tracks.addAll(files.map(::DesktopTrack))
                        queue.clear()
                        selectedTrack = tracks.firstOrNull()
                    }
                },
            )
            Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color(0xFF252A34))
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                HeaderBar(query = query, onQueryChange = { query = it })
                Row(modifier = Modifier.fillMaxSize()) {
                    TrackLibrary(
                        modifier = Modifier.weight(1.15f).fillMaxHeight(),
                        tracks = filteredTracks,
                        selectedTrack = selectedTrack,
                        onSelect = { selectedTrack = it },
                        onQueue = { if (!queue.contains(it)) queue += it },
                    )
                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color(0xFF252A34))
                    QueueAndPreview(
                        modifier = Modifier.weight(0.85f).fillMaxHeight(),
                        track = selectedTrack,
                        queue = queue,
                        onOpenTrack = { openInSystemPlayer(it.file) },
                        onRemoveFromQueue = { queue.remove(it) },
                        onPlayNext = {
                            val current = selectedTrack ?: return@QueueAndPreview
                            val idx = queue.indexOf(current)
                            selectedTrack = if (idx >= 0 && idx + 1 < queue.size) queue[idx + 1] else queue.firstOrNull()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationRailCard(
    modifier: Modifier = Modifier,
    currentFolder: File?,
    trackCount: Int,
    queueSize: Int,
    onPickFolder: () -> Unit,
    onImportFiles: () -> Unit,
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(Color(0xFF141925), Color(0xFF0D0F14))),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF7C4DFF).copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.35f)),
                ) {
                    Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Computer, contentDescription = null, tint = Color(0xFFD0BCFF))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("NeriPlayer Desktop", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("先做出一个能在 PC 上管理和打开本地音频的版本", color = Color(0xFF9AA4B2))
                }
            }

            StatusCard(label = "当前来源", value = currentFolder?.absolutePath ?: "已导入独立文件")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusCard(modifier = Modifier.weight(1f), label = "曲目", value = trackCount.toString())
                StatusCard(modifier = Modifier.weight(1f), label = "队列", value = queueSize.toString())
            }

            Button(onClick = onPickFolder, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 14.dp)) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("扫描整个音乐文件夹")
            }
            OutlinedButton(onClick = onImportFiles, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 14.dp)) {
                Icon(Icons.Rounded.LibraryMusic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("直接导入零散音频")
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151A22))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("PC 版首批能力", fontWeight = FontWeight.SemiBold)
                    Text("• 扫描文件夹中的常见音频格式\n• 搜索、选择、加入队列\n• 调用系统默认播放器立即播放", color = Color(0xFFB5BECC))
                    Text("后续如果要继续扩展，可以把 Android 的数据层逐步抽成共享模块。", color = Color(0xFF7F8A9A))
                }
            }
        }
    }
}

@Composable
private fun StatusCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF161B24))) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = Color(0xFF8E99A8), style = MaterialTheme.typography.labelMedium)
            Text(value, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun HeaderBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("本地媒体库", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("搜索歌曲 / 文件夹") },
            modifier = Modifier.width(320.dp),
            singleLine = true,
        )
    }
}

@Composable
private fun TrackLibrary(
    modifier: Modifier = Modifier,
    tracks: List<DesktopTrack>,
    selectedTrack: DesktopTrack?,
    onSelect: (DesktopTrack) -> Unit,
    onQueue: (DesktopTrack) -> Unit,
) {
    val listState = rememberLazyListState()
    Column(modifier = modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("曲目列表", style = MaterialTheme.typography.titleMedium, color = Color(0xFF9AA4B2))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF12161D)), modifier = Modifier.fillMaxSize()) {
            if (tracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("先选择一个文件夹，或者导入几首音频文件。", color = Color(0xFF8E99A8))
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tracks, key = { it.file.absolutePath }) { track ->
                        val selected = track == selectedTrack
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF20304A) else Color(0xFF1A202A)),
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(track) },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    Text(track.subtitle, color = Color(0xFF9AA4B2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                AssistChip(onClick = { onQueue(track) }, label = { Text("加入队列") }, leadingIcon = { Icon(Icons.Rounded.PlaylistPlay, contentDescription = null) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueAndPreview(
    modifier: Modifier = Modifier,
    track: DesktopTrack?,
    queue: List<DesktopTrack>,
    onOpenTrack: (DesktopTrack) -> Unit,
    onRemoveFromQueue: (DesktopTrack) -> Unit,
    onPlayNext: () -> Unit,
) {
    Column(modifier = modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("预览与队列", style = MaterialTheme.typography.titleMedium, color = Color(0xFF9AA4B2))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF12161D)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(track?.title ?: "还没有选中歌曲", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(track?.file?.absolutePath ?: "点击左侧曲目后，可在这里直接调用系统播放器。", color = Color(0xFF9AA4B2))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(enabled = track != null, onClick = { track?.let(onOpenTrack) }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("系统播放器打开")
                    }
                    OutlinedButton(enabled = queue.isNotEmpty(), onClick = onPlayNext) {
                        Text("切到队列下一首")
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF12161D)), modifier = Modifier.fillMaxSize()) {
            if (queue.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("队列还是空的，可以先把常听的歌放进来。", color = Color(0xFF8E99A8))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(queue, key = { it.file.absolutePath }) { trackItem ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A202A))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(trackItem.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(trackItem.subtitle, color = Color(0xFF9AA4B2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(onClick = { onRemoveFromQueue(trackItem) }) { Text("移除") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun chooseFolder(): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "选择音乐文件夹"
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isAcceptAllFileFilterUsed = false
    }
    return chooser.takeIf { it.showOpenDialog(null) == JFileChooser.APPROVE_OPTION }?.selectedFile
}

private fun chooseFiles(): List<File> {
    val chooser = JFileChooser().apply {
        dialogTitle = "导入音频文件"
        fileSelectionMode = JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = true
        fileFilter = FileNameExtensionFilter("音频文件", *supportedAudioExtensions.toTypedArray())
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFiles.toList()
    } else {
        emptyList()
    }
}

private fun scanAudioFiles(folder: File): List<DesktopTrack> =
    folder.walkTopDown()
        .filter { it.isFile && it.toPath().extension.lowercase() in supportedAudioExtensions }
        .map(::DesktopTrack)
        .sortedBy { it.title.lowercase() }
        .toList()

private fun openInSystemPlayer(file: File) {
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().open(file)
    }
}

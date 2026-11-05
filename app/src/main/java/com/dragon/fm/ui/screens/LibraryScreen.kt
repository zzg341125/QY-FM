package com.dragon.fm.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.dragon.fm.data.model.CustomScenarioMode
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.data.model.RecognizedSong
import com.dragon.fm.player.RecordedFile
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonGold
import com.dragon.fm.ui.theme.DragonTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Settings
import com.dragon.fm.ui.components.AboutBrandCard
import com.dragon.fm.ui.components.WechatQrDialog
import com.dragon.fm.ui.theme.LiveRed
import com.dragon.fm.ui.viewmodel.MainViewModel

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val tabs = listOf("我的收藏", "收听足迹", "录音库", "识曲足迹", "关于作者")
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val scope = rememberCoroutineScope()
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var showClearRecognitionConfirm by remember { mutableStateOf(false) }
    var showWechatDialog by remember { mutableStateOf(false) }
    var renamingStation by remember { mutableStateOf<RadioStation?>(null) }

    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val favoriteStations by viewModel.favoriteStations.collectAsState()
    val historyStations by viewModel.historyStations.collectAsState()
    val recordedFiles by viewModel.recordedFiles.collectAsState()
    val playingRecordedFile by viewModel.playingRecordedFile.collectAsState()
    val isLocalRecordingPlaying by viewModel.isLocalRecordingPlaying.collectAsState()
    val localRecordingPositionMs by viewModel.localRecordingPositionMs.collectAsState()
    val localRecordingDurationMs by viewModel.localRecordingDurationMs.collectAsState()
    val localRecordingSpeed by viewModel.localRecordingSpeed.collectAsState()
    val recognitionHistory by viewModel.recognitionHistory.collectAsState()

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 2) {
            viewModel.refreshRecordings()
        }
    }

    val configuration = LocalConfiguration.current
    val isExpanded = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || configuration.screenWidthDp >= 600
    val gridColumns = if (isExpanded) GridCells.Fixed(2) else GridCells.Fixed(1)

    val appBackground by viewModel.appBackground.collectAsState()
    val isDefaultBg = appBackground.type == com.dragon.fm.data.model.AppBackgroundType.DEFAULT

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isDefaultBg) Modifier.background(DragonTheme.background)
                else Modifier.background(Color.Transparent)
            )
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = DragonTheme.textPrimary
                    )
                }
                Text(
                    text = "我的电台馆",
                    color = DragonTheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DragonTheme.surface,
                    border = BorderStroke(0.8.dp, DragonTheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable { showWechatDialog = true }
                        .padding(end = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "关注公众号",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DragonTheme.primary
                        )
                    }
                }

                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "系统设置",
                        tint = DragonTheme.textPrimary
                    )
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = DragonTheme.primary,
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                            .height(3.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(DragonTheme.primary)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (pagerState.currentPage == index) DragonTheme.primary else DragonTheme.textSecondary
                        )
                    }
                )
            }
        }

        // Tab Contents with HorizontalPager for smooth swipe gestures
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
            0 -> {
                // Favorites Tab (Direct object persistence, never blanked out)
                if (favoriteStations.isEmpty()) {
                    EmptyStatePlaceholder(text = "暂无收藏电台，在播放页点击红心即可将心仪频道存入此处")
                } else {
                    LazyVerticalGrid(
                        columns = gridColumns,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        items(favoriteStations) { station ->
                            StationActionCard(
                                station = station,
                                isFavorite = true,
                                onPlay = {
                                    viewModel.playFavoritesPlaylist(station)
                                    onBack()
                                },
                                onPin = {
                                    viewModel.pinFavorite(station.id)
                                    Toast.makeText(context, "已置顶「${station.name}」", Toast.LENGTH_SHORT).show()
                                },
                                onRename = {
                                    renamingStation = station
                                },
                                onRemove = {
                                    viewModel.removeFavorite(station.id)
                                    Toast.makeText(context, "已取消收藏「${station.name}」", Toast.LENGTH_SHORT).show()
                                },
                                onBlock = {
                                    viewModel.blockStation(station)
                                    Toast.makeText(context, "已将「${station.name}」加入屏蔽黑名单", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
            1 -> {
                // History Footprint Tab (With clear-all and per-item deletion)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    if (historyStations.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "共 ${historyStations.size} 条收听足迹",
                                color = DragonTheme.textSecondary,
                                fontSize = 12.sp
                            )
                            Surface(
                                onClick = { showClearHistoryConfirm = true },
                                shape = RoundedCornerShape(8.dp),
                                color = LiveRed.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, LiveRed.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "清空足迹",
                                        tint = LiveRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "一键清空",
                                        color = LiveRed,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (historyStations.isEmpty()) {
                        EmptyStatePlaceholder(text = "收听足迹为空，去发现页漫游更多好声音吧")
                    } else {
                        LazyVerticalGrid(
                            columns = gridColumns,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(historyStations) { station ->
                                val isFav = favoriteIds.contains(station.id)
                                StationActionCard(
                                    station = station,
                                    isFavorite = isFav,
                                    onPlay = {
                                        viewModel.playStation(station)
                                        onBack()
                                    },
                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(station)
                                    },
                                    onRemove = {
                                        viewModel.removeFromHistory(station.id)
                                        Toast.makeText(context, "已删除足迹「${station.name}」", Toast.LENGTH_SHORT).show()
                                    },
                                    onBlock = {
                                        viewModel.blockStation(station)
                                        Toast.makeText(context, "已屏蔽「${station.name}」", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                // Recorded Files Tab
                Box(modifier = Modifier.fillMaxSize()) {
                    if (recordedFiles.isEmpty()) {
                        EmptyStatePlaceholder(text = "暂无电台录音，在播放卡片点击“录音”即可录制精彩节目")
                    } else {
                        LazyVerticalGrid(
                            columns = gridColumns,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = if (playingRecordedFile != null) 130.dp else 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                        ) {
                            items(recordedFiles) { rec ->
                                val isActive = playingRecordedFile?.file?.absolutePath == rec.file.absolutePath
                                val isPlaying = isActive && isLocalRecordingPlaying
                                RecordingFileCard(
                                    record = rec,
                                    isPlaying = isPlaying,
                                    isActive = isActive,
                                    onPlayToggle = {
                                        if (isActive) {
                                            viewModel.toggleRecordedFilePlayPause()
                                        } else {
                                            viewModel.playRecordedFile(rec)
                                        }
                                    },
                                    onDelete = { viewModel.deleteRecordedFile(rec) }
                                )
                            }
                        }
                    }

                    // Floating In-App Recording Player Bar with Seek & Speed Controls
                    if (playingRecordedFile != null) {
                        FloatingRecordingPlayerBar(
                            file = playingRecordedFile!!,
                            isPlaying = isLocalRecordingPlaying,
                            positionMs = localRecordingPositionMs,
                            durationMs = localRecordingDurationMs,
                            speed = localRecordingSpeed,
                            onTogglePlay = { viewModel.toggleRecordedFilePlayPause() },
                            onSeek = { viewModel.seekRecordedFile(it) },
                            onCycleSpeed = { viewModel.cycleRecordedFileSpeed() },
                            onClose = { viewModel.stopRecordedFile() },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
            3 -> {
                // Recognition History Tab (识曲足迹)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    if (recognitionHistory.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "共识别 ${recognitionHistory.size} 首歌曲",
                                color = DragonTheme.textSecondary,
                                fontSize = 12.sp
                            )
                            Surface(
                                onClick = { showClearRecognitionConfirm = true },
                                shape = RoundedCornerShape(8.dp),
                                color = LiveRed.copy(alpha = 0.12f),
                                border = BorderStroke(0.6.dp, LiveRed.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear",
                                        tint = LiveRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "清空识曲",
                                        color = LiveRed,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (recognitionHistory.isEmpty()) {
                        EmptyStatePlaceholder(text = "暂无音乐识别足迹，在播放页点击「识曲」即可识别心仪歌曲")
                    } else {
                        LazyVerticalGrid(
                            columns = gridColumns,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(recognitionHistory, key = { it.id }) { song ->
                                RecognizedSongCard(
                                    song = song,
                                    onCopy = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("song", "${song.title} - ${song.artist}"))
                                        Toast.makeText(context, "已复制: ${song.title} - ${song.artist}", Toast.LENGTH_SHORT).show()
                                    },
                                    onSearch = {
                                        try {
                                            val targetUrl = if (song.shazamUrl.isNotBlank()) {
                                                song.shazamUrl
                                            } else {
                                                "https://www.shazam.com/search?q=" + Uri.encode("${song.title} ${song.artist}")
                                            }
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "无法打开 Shazam 对应页面", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = {
                                        viewModel.deleteRecognizedSong(song.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            4 -> {
                // 关于作者与微信公众号宣传
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AboutBrandCard()
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
    }

    // Add Custom Station Dialog
    if (showAddCustomDialog) {
        var nameInput by remember { mutableStateOf("") }
        var urlInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            containerColor = DragonTheme.surface,
            title = { Text("添加自定义电台流", color = DragonTheme.textPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("电台名称 (如: 私人电台)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("流地址 URL (http/https/m3u8)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank() && urlInput.isNotBlank()) {
                            viewModel.addCustomStation(nameInput, urlInput)
                            showAddCustomDialog = false
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DragonCyan, contentColor = Color(0xFF070B12))
                ) {
                    Text("添加并播放", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text("取消", color = DragonTheme.textSecondary)
                }
            }
        )
    }

    // Clear History Confirmation Dialog
    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            containerColor = DragonTheme.surface,
            title = { Text("清空收听足迹", color = DragonTheme.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "确定要清空全部收听足迹吗？此操作无法撤销。",
                    color = DragonTheme.textSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryConfirm = false
                        Toast.makeText(context, "收听足迹已全部清空", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                ) {
                    Text("确认清空", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("取消", color = DragonTheme.textSecondary)
                }
            }
        )
    }

    // Wechat Official Account Dialog
    if (showWechatDialog) {
        WechatQrDialog(onDismiss = { showWechatDialog = false })
    }

    // Clear Recognition History Confirmation Dialog
    if (showClearRecognitionConfirm) {
        AlertDialog(
            onDismissRequest = { showClearRecognitionConfirm = false },
            containerColor = DragonTheme.surface,
            title = { Text("清空识曲足迹", color = DragonTheme.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "确定要清空全部识别出的歌曲记录吗？此操作无法撤销。",
                    color = DragonTheme.textSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearRecognitionHistory()
                        showClearRecognitionConfirm = false
                        Toast.makeText(context, "识曲足迹已全部清空", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                ) {
                    Text("确认清空", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearRecognitionConfirm = false }) {
                    Text("取消", color = DragonTheme.textSecondary)
                }
            }
        )
    }

    // Custom Station Name Rename Dialog
    if (renamingStation != null) {
        val target = renamingStation!!
        var text by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renamingStation = null },
            containerColor = DragonTheme.surface,
            title = {
                Text(
                    text = "自定义频道名称",
                    color = DragonTheme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "给此电台设置个性化显示名称，各播放列表与正在播放中均将同步更新：",
                        color = DragonTheme.textSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        placeholder = { Text("输入自定义频道名称", color = DragonTheme.textSecondary.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DragonTheme.primary,
                            unfocusedBorderColor = DragonTheme.glassBorder,
                            focusedTextColor = DragonTheme.textPrimary,
                            unfocusedTextColor = DragonTheme.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = text.trim()
                        if (newName.isNotBlank()) {
                            viewModel.renameFavoriteStation(target.id, newName)
                            Toast.makeText(context, "已重命名为「$newName」", Toast.LENGTH_SHORT).show()
                        }
                        renamingStation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DragonTheme.primary)
                ) {
                    Text("保存", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.renameFavoriteStation(target.id, null)
                            Toast.makeText(context, "已恢复原台名", Toast.LENGTH_SHORT).show()
                            renamingStation = null
                        }
                    ) {
                        Text("恢复默认", color = LiveRed, fontSize = 13.sp)
                    }
                    TextButton(onClick = { renamingStation = null }) {
                        Text("取消", color = DragonTheme.textSecondary, fontSize = 13.sp)
                    }
                }
            }
        )
    }
}

/**
 * Universal Card for Favorites and Footprints with 3-dot Menu
 */
@Composable
private fun StationActionCard(
    station: RadioStation,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onPin: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onRemove: () -> Unit,
    onBlock: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = DragonTheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DragonTheme.glassBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DragonTheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!station.favicon.isNullOrBlank()) {
                    AsyncImage(
                        model = station.favicon,
                        contentDescription = station.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "Station",
                        tint = DragonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Station Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    color = DragonTheme.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (station.hasDedicatedLine) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.45f)),
                            modifier = Modifier.padding(end = 5.dp)
                        ) {
                            Text(
                                text = "专线",
                                color = Color(0xFF00E5FF),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                            )
                        }
                    }
                    val region = station.formattedRegion
                    val desc = station.currentProgram ?: (if (station.tags.isNotBlank()) "$region · ${station.tags.take(16)}" else region)
                    Text(
                        text = desc,
                        color = if (station.currentProgram != null) DragonTheme.primary.copy(alpha = 0.85f) else DragonTheme.textSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Play Icon
            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = DragonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 3-Dots More Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多设置",
                        tint = DragonTheme.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(DragonTheme.surface)
                ) {
                    if (onPin != null) {
                        DropdownMenuItem(
                            text = { Text("置顶到最前", color = DragonTheme.textPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.PushPin, contentDescription = null, tint = DragonGold, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                menuExpanded = false
                                onPin()
                            }
                        )
                    }

                    if (onRename != null) {
                        DropdownMenuItem(
                            text = { Text("自定义频道名称", color = DragonTheme.textPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = DragonCyan, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            }
                        )
                    }

                    if (onToggleFavorite != null) {
                        DropdownMenuItem(
                            text = { Text(if (isFavorite) "取消收藏" else "加入收藏", color = DragonTheme.textPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = LiveRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleFavorite()
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("移除记录", color = LiveRed, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = LiveRed, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("屏蔽此电台", color = DragonTheme.textSecondary, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Block, contentDescription = null, tint = DragonTheme.textSecondary, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onBlock()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingFileCard(
    record: RecordedFile,
    isPlaying: Boolean = false,
    isActive: Boolean = false,
    onPlayToggle: () -> Unit = {},
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val sizeMb = String.format("%.2f MB", record.sizeBytes / (1024f * 1024f))

    Surface(
        onClick = onPlayToggle,
        color = if (isActive) DragonTheme.surfaceVariant else DragonTheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, if (isActive) DragonCyan else DragonTheme.glassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isActive) DragonCyan.copy(alpha = 0.22f) else Color(0x3300F5D4)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onPlayToggle, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = DragonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.stationName,
                    color = if (isActive) DragonCyan else DragonTheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.dateString} • $sizeMb",
                    color = DragonTheme.textSecondary,
                    fontSize = 12.sp
                )
            }

            // Share Action
            IconButton(
                onClick = {
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            record.file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享录音音频"))
                    } catch (_: Exception) {}
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = DragonTheme.textSecondary
                )
            }

            // Delete Action
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = LiveRed.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun FloatingRecordingPlayerBar(
    file: RecordedFile,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    speed: Float,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (DragonTheme.isDark) Color(0xF2111827) else DragonTheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, DragonCyan.copy(alpha = 0.5f)),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header Row: Info & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DragonCyan.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = DragonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = file.stationName,
                            color = DragonTheme.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = file.dateString,
                            color = DragonTheme.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                // Speed Switcher Pill
                Surface(
                    onClick = onCycleSpeed,
                    shape = RoundedCornerShape(8.dp),
                    color = DragonCyan.copy(alpha = 0.15f),
                    border = BorderStroke(0.6.dp, DragonCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = DragonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${speed}x",
                            color = DragonCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play / Pause Circle
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DragonCyan)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF070B12),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Close Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = DragonTheme.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Seekbar Slider
            val progressFraction = if (durationMs > 0L) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progressFraction,
                onValueChange = { fraction ->
                    onSeek((fraction * durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = DragonCyan,
                    activeTrackColor = DragonCyan,
                    inactiveTrackColor = DragonTheme.glassBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )

            // Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatRecordTime(positionMs),
                    color = DragonTheme.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = formatRecordTime(durationMs),
                    color = DragonTheme.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

private fun formatRecordTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%02d:%02d", m, s)
}

@Composable
private fun EmptyStatePlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = DragonTheme.textSecondary,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun RecognizedSongCard(
    song: RecognizedSong,
    onCopy: () -> Unit,
    onSearch: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = remember { java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()) }
    val dateStr = remember(song.timestamp) { sdf.format(java.util.Date(song.timestamp)) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DragonTheme.surface,
        border = BorderStroke(1.dp, DragonTheme.glassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Art
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DragonTheme.background)
                    .border(0.8.dp, DragonTheme.glassBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (song.coverArtUrl.isNotBlank()) {
                    AsyncImage(
                        model = song.coverArtUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = DragonTheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DragonTheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist + if (song.album.isNotBlank()) " • 《${song.album}》" else "",
                    fontSize = 12.5.sp,
                    color = DragonTheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${if (song.stationName.isNotBlank()) "来源: ${song.stationName} • " else ""}$dateStr",
                    fontSize = 11.sp,
                    color = DragonTheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Copy Action
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = DragonTheme.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Search Action
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = "Search",
                    tint = DragonTheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete Action
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = LiveRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

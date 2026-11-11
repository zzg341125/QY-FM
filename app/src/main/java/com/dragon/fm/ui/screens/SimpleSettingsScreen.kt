package com.dragon.fm.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.data.api.WebDavManager
import com.dragon.fm.data.model.PlaybackStatus
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.data.model.UpdateCheckResult
import com.dragon.fm.ui.theme.ThemePreset
import com.dragon.fm.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 极简版设置页，界面参照用户截图：
 * 常规 / 维护 / 高级 / 关于，顶部带“搜索电台”栏，底部固定正在播放条。
 */
@Composable
fun SimpleSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val playerState by viewModel.playerState.collectAsState()
    val playlist by viewModel.playlist.collectAsState()

    val themeId by viewModel.simpleThemeId.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val transparentUi by viewModel.transparentUi.collectAsState()
    val clickStationToPlay by viewModel.clickStationToPlay.collectAsState()
    val largeBuffer by viewModel.largeBuffer.collectAsState()
    val editStationEnabled by viewModel.editStationEnabled.collectAsState()
    val editStreamUrlEnabled by viewModel.editStreamUrlEnabled.collectAsState()
    val autoPlayOnLaunch by viewModel.isAutoPlayOnLaunch.collectAsState()
    val playbackMemory by viewModel.playbackMemory.collectAsState()
    val fastStart by viewModel.fastStart.collectAsState()
    val pauseOnUnplug by viewModel.pauseOnUnplug.collectAsState()
    val autoUpdateEnabled by viewModel.autoUpdateEnabled.collectAsState()
    val updateCheckResult by viewModel.updateCheckResult.collectAsState()

    val searchResults by viewModel.stationSearchResults.collectAsState()
    val isSearching by viewModel.isStationSearching.collectAsState()
    val sleepRemaining by viewModel.sleepTimerSeconds.collectAsState()

    val preset = remember(themeId) { ThemePreset.fromId(themeId) }
    val colors = remember(preset) { simpleColors(preset) }
    ApplySystemBarAppearance(preset.isDark)

    var query by remember { mutableStateOf("") }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showWebDavDialog by remember { mutableStateOf(false) }

    LaunchedEffect(updateCheckResult) {
        when (val result = updateCheckResult) {
            is UpdateCheckResult.UpToDate -> if (result.isManual) {
                Toast.makeText(
                    context,
                    "当前已是最新版本 (v${result.currentVersion})",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is UpdateCheckResult.Error -> if (result.isManual) {
                Toast.makeText(context, "检测更新失败：${result.message}", Toast.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            viewModel.searchStationsForAdd("")
        } else {
            delay(400)
            viewModel.searchStationsForAdd(query)
        }
    }

    val importM3uLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val count = viewModel.importM3uPlaylist(readTextFromUri(context, uri))
            Toast.makeText(
                context,
                if (count > 0) "已导入 $count 个电台" else "未找到可导入的电台",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val exportM3uLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri: Uri? ->
        if (uri != null) {
            val ok = writeTextToUri(context, uri, viewModel.buildM3uPlaylist())
            Toast.makeText(context, if (ok) "M3U 已导出" else "导出失败", Toast.LENGTH_SHORT).show()
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            val json = viewModel.buildBackupJson()
            val ok = json != null && writeTextToUri(context, uri, json)
            Toast.makeText(context, if (ok) "电台已备份" else "备份失败", Toast.LENGTH_SHORT).show()
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val count = viewModel.restoreFromBackupJson(readTextFromUri(context, uri))
            Toast.makeText(
                context,
                when {
                    count > 0 -> "已恢复 $count 个电台"
                    count == 0 -> "备份文件为空"
                    else -> "备份文件格式不正确"
                },
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "设置",
                color = colors.textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                SearchStationSection(
                    query = query,
                    onQueryChange = { query = it },
                    results = searchResults,
                    isSearching = isSearching,
                    playlist = playlist,
                    colors = colors,
                    onAdd = { station -> viewModel.addStationToPlaylist(station) }
                )
            }

            item { SettingsSectionTitle("常规", colors) }

            item {
                SettingsActionRow(
                    icon = Icons.Outlined.Palette,
                    title = "应用主题",
                    subtitle = "当前主题：${preset.title}",
                    colors = colors,
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Palette,
                    title = "动态颜色",
                    subtitle = "用户界面元素使用系统强调色。",
                    checked = dynamicColor,
                    colors = colors,
                    onCheckedChange = { viewModel.setDynamicColor(it) }
                )
            }
            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Opacity,
                    title = "用户界面透明效果",
                    subtitle = "滚动时播放控件保持不透明。",
                    checked = transparentUi,
                    colors = colors,
                    onCheckedChange = { viewModel.setTransparentUi(it) }
                )
            }
            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.PlayCircle,
                    title = "点击广播电台",
                    subtitle = "点击广播电台任意部位开始收听。",
                    checked = clickStationToPlay,
                    colors = colors,
                    onCheckedChange = { viewModel.setClickStationToPlay(it) }
                )
            }
            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.PlayCircle,
                    title = "启动时自动播放",
                    subtitle = "打开应用后自动播放上次收听的电台。",
                    checked = autoPlayOnLaunch,
                    colors = colors,
                    onCheckedChange = { viewModel.setAutoPlayOnLaunch(it) }
                )
            }
            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Restore,
                    title = "记忆播放列表与进度",
                    subtitle = "保存当前列表、播放位置，下次打开自动恢复。",
                    checked = playbackMemory,
                    colors = colors,
                    onCheckedChange = { viewModel.setPlaybackMemory(it) }
                )
            }

            item { SettingsDivider(colors) }
            item { SettingsSectionTitle("维护", colors) }

            item {
                SettingsActionRow(
                    icon = Icons.Outlined.Image,
                    title = "更新电台图标",
                    subtitle = "重新加载所有电台的最新图标。",
                    colors = colors,
                    onClick = { viewModel.refreshStationIcons() }
                )
            }
            item {
                SettingsActionRow(
                    icon = Icons.Outlined.PlaylistPlay,
                    title = "导出 M3U",
                    subtitle = "将您的广播电台保存到 M3U 播放列表——可被导入到其它播放器。",
                    colors = colors,
                    onClick = { exportM3uLauncher.launch("收音机_电台列表.m3u") }
                )
            }
            item {
                SettingsActionRow(
                    icon = Icons.Outlined.PlaylistPlay,
                    title = "导入 M3U",
                    subtitle = "从 M3U / M3U8 播放列表恢复电台，导入后追加到播放主界面。",
                    colors = colors,
                    onClick = {
                        importM3uLauncher.launch(
                            arrayOf(
                                "audio/x-mpegurl",
                                "audio/mpegurl",
                                "application/x-mpegurl",
                                "text/plain",
                                "*/*"
                            )
                        )
                    }
                )
            }
            item {
                SettingsActionRow(
                    icon = Icons.Outlined.FileDownload,
                    title = "备份电台",
                    subtitle = "将包括图标在内的广播电台收藏保存到设备存储中。",
                    colors = colors,
                    onClick = { backupLauncher.launch("LXFM_电台备份.json") }
                )
            }
            item {
                SettingsActionRow(
                    icon = Icons.Outlined.Restore,
                    title = "恢复电台",
                    subtitle = "从本地备份中恢复广播电台收藏。现有电台将被替换。",
                    colors = colors,
                    onClick = {
                        restoreLauncher.launch(
                            arrayOf("application/json", "text/plain", "*/*")
                        )
                    }
                )
            }
            item {
                SettingsActionRow(
                    icon = Icons.Outlined.CloudUpload,
                    title = "在线备份 (WebDAV)",
                    subtitle = "备份播放列表、上下顺序、设置与台标；恢复后保持原顺序。",
                    colors = colors,
                    onClick = { showWebDavDialog = true }
                )
            }

            item { SettingsDivider(colors) }
            item { SettingsSectionTitle("高级", colors) }

            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Tune,
                    title = "0.25s 极速起播优化",
                    subtitle = "使用 0.25 秒快速起播缓冲，牺牲少量稳定性换取秒开体验（重启应用后生效）。",
                    checked = fastStart,
                    colors = colors,
                    onCheckedChange = { viewModel.setFastStart(it) }
                )
            }
            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Tune,
                    title = "使用更大缓冲",
                    subtitle = "使用更大的缓冲大小播放电台流媒体（重启应用后生效）。",
                    checked = largeBuffer,
                    colors = colors,
                    onCheckedChange = { viewModel.setLargeBuffer(it) }
                )
            }
            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Edit,
                    title = "编辑电台",
                    subtitle = "已启用电台信息编辑。长按进入编辑模式。",
                    checked = editStationEnabled,
                    colors = colors,
                    onCheckedChange = { viewModel.setEditStationEnabled(it) }
                )
            }
            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Link,
                    title = "编辑流媒体链接",
                    subtitle = "已启用流媒体链接编辑。请确保输入地址正确。",
                    checked = editStreamUrlEnabled,
                    colors = colors,
                    enabled = editStationEnabled,
                    onCheckedChange = { viewModel.setEditStreamUrlEnabled(it) }
                )
            }
            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Link,
                    title = "拔出耳机自动暂停",
                    subtitle = "拔出耳机或断开蓝牙时自动暂停播放，避免外放打扰。",
                    checked = pauseOnUnplug,
                    colors = colors,
                    onCheckedChange = { viewModel.setPauseOnUnplug(it) }
                )
            }

            item { SettingsDivider(colors) }
            item { SettingsSectionTitle("关于", colors) }
            item {
                SettingsActionRow(
                    icon = Icons.Outlined.Restore,
                    title = if (updateCheckResult is UpdateCheckResult.Checking) "正在检查更新" else "检查更新",
                    subtitle = "当前版本 ${viewModel.currentVersionName} · GitHub Releases",
                    colors = colors,
                    onClick = { viewModel.checkForUpdates(isManual = true) }
                )
            }
            item {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Info,
                    title = "自动检查并下载更新",
                    subtitle = "启动后自动检查 GitHub 新版本并下载，安装时由 Android 系统确认。",
                    checked = autoUpdateEnabled,
                    colors = colors,
                    onCheckedChange = { viewModel.setAutoUpdateEnabled(it) }
                )
            }
            item {
                SettingsActionRow(
                    icon = Icons.Outlined.Link,
                    title = "GitHub 开源项目",
                    subtitle = "github.com/zzg341125/QY-FM",
                    colors = colors,
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/zzg341125/QY-FM")
                                )
                            )
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                "请在浏览器访问 github.com/zzg341125/QY-FM",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bottomBar)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            SimpleNowPlayingBar(
                station = playlist.firstOrNull { it.id == playerState.currentStation?.id }
                    ?: playerState.currentStation,
                title = playerState.nowPlayingTitle.ifBlank {
                    playerState.currentStation?.currentProgram.orEmpty()
                },
                isPlaying = playerState.status == PlaybackStatus.PLAYING,
                colors = colors,
                transparent = transparentUi,
                sleepRemaining = sleepRemaining,
                onTogglePlay = { viewModel.togglePlayPause() }
            )
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = colors.fieldBg,
            shape = RoundedCornerShape(22.dp),
            title = { Text("应用主题", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    ThemePreset.entries.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setSimpleThemeId(item.id)
                                    showThemeDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(item.primary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = colors.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = item.subtitle,
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (themeId == item.id) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = colors.playButton,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("关闭", color = colors.playButton, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showWebDavDialog) {
        WebDavBackupDialog(
            viewModel = viewModel,
            colors = colors,
            onDismiss = { showWebDavDialog = false }
        )
    }
}

@Composable
private fun SearchStationSection(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<RadioStation>,
    isSearching: Boolean,
    playlist: List<RadioStation>,
    colors: SimpleColors,
    onAdd: (RadioStation) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = "搜索电台",
            color = colors.textSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("输入电台名称 / 地区 / 频率", color = colors.textSecondary, fontSize = 15.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.fieldBg,
                unfocusedContainerColor = colors.fieldBg,
                focusedBorderColor = colors.playButton,
                unfocusedBorderColor = colors.divider,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.playButton
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (isSearching) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = colors.playButton
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("正在搜索…", color = colors.textSecondary, fontSize = 14.sp)
            }
        }

        if (query.isNotBlank() && !isSearching && results.isEmpty()) {
            Text(
                text = "没有找到相关电台，换个关键词试试",
                color = colors.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }

        results.take(8).forEach { station ->
            val alreadyAdded = playlist.any { it.id == station.id }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !alreadyAdded) { onAdd(station) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOfNotNull(
                            station.country.takeIf { it.isNotBlank() },
                            station.state?.takeIf { it.isNotBlank() }
                        ).joinToString(" · ").ifBlank { "网络电台" },
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (alreadyAdded) colors.divider else colors.playButton),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (alreadyAdded) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = if (alreadyAdded) "已添加" else "添加",
                        tint = if (alreadyAdded) colors.textSecondary else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String, colors: SimpleColors) {
    Text(
        text = title,
        color = colors.textSecondary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsDivider(colors: SimpleColors) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(0.7.dp)
            .background(colors.divider)
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    colors: SimpleColors,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier
                .padding(top = 4.dp)
                .size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = colors.textSecondary,
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.playButton,
                uncheckedThumbColor = if (colors.isDark) Color(0xFFB9BEC7) else Color(0xFF30343A),
                uncheckedTrackColor = if (colors.isDark) Color(0xFF3A3D44) else Color(0xFFD8DBE0),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    colors: SimpleColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier
                .padding(top = 4.dp)
                .size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = colors.textSecondary,
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
        }
    }
}

private fun themeModeLabel(mode: String): String = when (mode) {
    "light" -> "浅色"
    "dark" -> "深色"
    else -> "跟随设备"
}

/** 读取文本文件（优先 UTF-8，含乱码时回退 GBK） */
private fun readTextFromUri(context: Context, uri: Uri): String {
    return try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return ""
        val utf8 = String(bytes, Charsets.UTF_8)
        if (utf8.contains('\uFFFD')) {
            runCatching { String(bytes, charset("GBK")) }.getOrDefault(utf8)
        } else {
            utf8
        }
    } catch (_: Exception) {
        ""
    }
}

/** 写入文本文件 */
private fun writeTextToUri(context: Context, uri: Uri, text: String): Boolean {
    return try {
        context.contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(text.toByteArray(Charsets.UTF_8))
        }
        true
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun webDavFieldColors(colors: SimpleColors) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = colors.textPrimary,
    unfocusedTextColor = colors.textPrimary,
    focusedLabelColor = colors.playButton,
    unfocusedLabelColor = colors.textSecondary,
    cursorColor = colors.playButton,
    focusedBorderColor = colors.playButton,
    unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.55f),
    focusedContainerColor = colors.fieldBg,
    unfocusedContainerColor = colors.fieldBg
)

/**
 * WebDAV 在线备份对话框：输入地址、用户名、密码，上传/恢复备份。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WebDavBackupDialog(
    viewModel: MainViewModel,
    colors: SimpleColors,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf(viewModel.webDavUrl()) }
    var user by remember { mutableStateOf(viewModel.webDavUser()) }
    var password by remember { mutableStateOf(viewModel.webDavPassword()) }
    var fileName by remember {
        mutableStateOf(viewModel.webDavFileName().removeSuffix(".zip").removeSuffix(".ZIP"))
    }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var remoteFiles by remember { mutableStateOf<List<WebDavManager.RemoteFile>>(emptyList()) }
    var showPicker by remember { mutableStateOf(false) }
    val backupDir = remember(url) { url.trim() }

    // 自动记住地址 / 用户名 / 密码，下次打开无需重填
    LaunchedEffect(url, user, password, fileName) {
        delay(300)
        viewModel.saveWebDavConfig(url, user, password, fileName)
    }

    fun runAction(block: suspend () -> String) {
        if (busy) return
        if (url.isBlank()) {
            status = "请填写 WebDAV 服务器地址"
            return
        }
        viewModel.saveWebDavConfig(url, user, password, fileName)
        busy = true
        status = "处理中…"
        scope.launch {
            val message = try {
                block()
            } catch (e: Exception) {
                "失败：${e.message ?: "未知错误"}"
            }
            status = message
            busy = false
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = colors.background,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "在线备份 (WebDAV)",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("服务器地址") },
                    placeholder = {
                        Text(
                            "https://dav.example.com/remote.php/dav/files/用户名/",
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    colors = webDavFieldColors(colors),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("用户名") },
                    singleLine = true,
                    colors = webDavFieldColors(colors),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码 / 应用密码（明文显示）") },
                    singleLine = true,
                    colors = webDavFieldColors(colors),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("文件名前缀（上传时自动加日期）") },
                    placeholder = { Text("quyang_backup", fontSize = 12.sp) },
                    singleLine = true,
                    colors = webDavFieldColors(colors),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "每次上传都会新增一个 ${fileName.trim().ifBlank { "quyang_backup" }}_日期_时间.zip，包含台标与电台顺序",
                    color = colors.textSecondary.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )

                if (status.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = status,
                        color = if (status.startsWith("失败")) {
                            if (colors.isDark) Color(0xFFFF8A80) else Color(0xFFD6473B)
                        } else {
                            colors.textSecondary
                        },
                        fontSize = 13.sp
                    )
                }
                if (backupDir.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "备份目录：$backupDir",
                        color = colors.textSecondary.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            runAction {
                                withContext(Dispatchers.IO) {
                                    WebDavManager.testConnection(url, user, password)
                                        .fold(
                                            { "连接成功（HTTP $it）" },
                                            { "失败：${it.message}" }
                                        )
                                }
                            }
                        }
                    ) {
                        Text("测试连接", color = colors.playButton, fontSize = 13.sp)
                    }
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            runAction {
                                withContext(Dispatchers.IO) {
                                    WebDavManager.inspect(url, user, password)
                                }.fold(
                                    { info ->
                                        buildString {
                                            append("服务器：")
                                            append(info.server.ifBlank { "未知" })
                                            if (info.allow.isNotEmpty()) {
                                                append("\n支持方法：")
                                                append(info.allow.joinToString(", "))
                                            }
                                            if (info.dav.isNotBlank()) {
                                                append("\nDAV：")
                                                append(info.dav)
                                            }
                                            append("\n目录项 ").append(info.hrefs.size).append(" 个")
                                            info.hrefs.take(4).forEach {
                                                append("\n").append(it.take(80))
                                            }
                                        }
                                    },
                                    { "失败：${it.message}" }
                                )
                            }
                        }
                    ) {
                        Text("查看目录", color = colors.playButton, fontSize = 13.sp)
                    }
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            runAction {
                                val zip = withContext(Dispatchers.IO) { viewModel.buildBackupZip() }
                                    ?: return@runAction "失败：打包备份失败"
                                val bytes = withContext(Dispatchers.IO) { zip.readBytes() }
                                val uploadName = WebDavManager.backupFileName(fileName)
                                withContext(Dispatchers.IO) {
                                    WebDavManager.uploadSmart(url, uploadName, user, password, bytes)
                                }.fold(
                                    { usedUrl ->
                                        "已备份 ${viewModel.playlist.value.size} 个电台及台标（${bytes.size / 1024} KB）\n$usedUrl"
                                    },
                                    { "失败：${it.message}" }
                                )
                            }
                        }
                    ) {
                        Text("上传备份", color = colors.playButton, fontSize = 13.sp)
                    }
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            runAction {
                                val files = withContext(Dispatchers.IO) {
                                    WebDavManager.listFiles(url, user, password)
                                }.getOrElse { return@runAction "失败：${it.message}" }
                                if (files.isEmpty()) {
                                    return@runAction "失败：云端没有找到 .zip 备份文件"
                                }
                                remoteFiles = files
                                showPicker = true
                                "找到 ${files.size} 个备份，请选择要恢复的"
                            }
                        }
                    ) {
                        Text("从云端恢复", color = colors.playButton, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("关闭", color = colors.textSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            containerColor = colors.background,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "选择要恢复的备份",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    remoteFiles.forEach { file ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showPicker = false
                                    runAction {
                                        val bytes = withContext(Dispatchers.IO) {
                                            WebDavManager.download(file.url, user, password)
                                        }.getOrElse { return@runAction "失败：${it.message}" }
                                        val temp = File(context.cacheDir, "webdav_restore.zip")
                                        withContext(Dispatchers.IO) { temp.writeBytes(bytes) }
                                        val count = viewModel.restoreBackupZip(temp)
                                        "已恢复 ${file.name}：$count 个电台与设置"
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = file.name,
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (file.size > 0) "${file.size / 1024} KB" else "—",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("取消", color = colors.textSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

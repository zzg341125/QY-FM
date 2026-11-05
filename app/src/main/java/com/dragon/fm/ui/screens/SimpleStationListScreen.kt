package com.dragon.fm.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dragon.fm.data.model.PlaybackStatus
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.ui.viewmodel.MainViewModel

private val BgColor = Color(0xFFF7F8FA)
private val BottomBarColor = Color(0xFFF2F3F6)
private val TextPrimary = Color(0xFF17181A)
private val TextSecondary = Color(0xFF6B7078)
private val DividerColor = Color(0xFFE8EAEE)
private val OutlineColor = Color(0xFF2B2C2E)
private val NowPlayingBg = Color(0xFFE7EEF9)
private val PlayButtonColor = Color(0xFF56698C)

/**
 * 极简纯净版首页：电台列表 + 添加电台 + 设置 + 底部正在播放条。
 * 打开应用即自动播放，并直接展示该页面。
 */
@Composable
fun SimpleStationListScreen(viewModel: MainViewModel) {
    val playlist by viewModel.playlist.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val detectedProvince by viewModel.detectedProvince.collectAsState()
    val detectedCity by viewModel.detectedCity.collectAsState()

    val listState = rememberLazyListState()
    val context = LocalContext.current

    var showAddStation by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val currentStation = playerState.currentStation
    val currentId = currentStation?.id

    // 当前播放电台变化时，自动滚动到对应条目
    LaunchedEffect(currentId, playlist.size) {
        val index = playlist.indexOfFirst { it.id == currentId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState
        ) {
            itemsIndexed(playlist, key = { _, station -> station.id }) { index, station ->
                StationRow(
                    station = station,
                    isCurrent = station.id == currentId,
                    isPlaying = station.id == currentId && playerState.status == PlaybackStatus.PLAYING,
                    onPlay = { viewModel.playStationAtIndex(index) }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.7.dp)
                        .background(DividerColor)
                )
            }
            item { Spacer(modifier = Modifier.height(6.dp)) }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BottomBarColor)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PillActionButton(
                    text = "添加新电台",
                    icon = { tint -> Icon(Icons.Default.Add, contentDescription = "添加新电台", tint = tint, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1.5f),
                    onClick = { showAddStation = true }
                )
                PillActionButton(
                    text = "设置",
                    icon = { tint -> Icon(Icons.Default.Settings, contentDescription = "设置", tint = tint, modifier = Modifier.size(17.dp)) },
                    modifier = Modifier.weight(0.8f),
                    onClick = { showSettings = true }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            NowPlayingBar(
                station = currentStation,
                title = playerState.nowPlayingTitle.ifBlank { currentStation?.currentProgram.orEmpty() },
                isPlaying = playerState.status == PlaybackStatus.PLAYING,
                onTogglePlay = { viewModel.togglePlayPause() }
            )
        }
    }

    if (showAddStation) {
        AddStationDialog(
            onDismiss = { showAddStation = false },
            onConfirm = { name, url ->
                viewModel.addCustomStation(name, url)
                showAddStation = false
            }
        )
    }

    if (showSettings) {
        SettingsDialog(
            versionName = viewModel.currentVersionName,
            onRefreshLocal = {
                viewModel.loadLocalScenarioStations(detectedProvince, detectedCity)
                showSettings = false
            },
            onCheckUpdate = {
                viewModel.checkForUpdates(isManual = true)
                showSettings = false
            },
            onExit = {
                showSettings = false
                (context as? Activity)?.finish()
            },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun StationRow(
    station: RadioStation,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = station.favicon,
            contentDescription = station.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFEFEF))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = station.name,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        PlayCircleButton(
            isPlaying = isPlaying,
            highlighted = isCurrent,
            onClick = onPlay
        )
    }
}

@Composable
private fun PlayCircleButton(
    isPlaying: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(1.2.dp, if (highlighted) PlayButtonColor else OutlineColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "暂停" else "播放",
            tint = if (highlighted) PlayButtonColor else OutlineColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun PillActionButton(
    text: String,
    icon: @Composable (Color) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFFF0F1F4),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9DCE1)),
        modifier = modifier.height(52.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon(TextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NowPlayingBar(
    station: RadioStation?,
    title: String,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit
) {
    Surface(
        onClick = onTogglePlay,
        shape = RoundedCornerShape(30.dp),
        color = NowPlayingBg,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = station?.favicon,
                contentDescription = station?.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title.ifBlank { station?.name ?: "暂未选择电台" },
                    color = PlayButtonColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = station?.name ?: "点击列表开始收听",
                    color = PlayButtonColor.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PlayButtonColor)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun AddStationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val canSave = name.isNotBlank() && url.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.Radio, contentDescription = null, tint = PlayButtonColor) },
        title = { Text("添加新电台", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("电台名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("直播流地址 (http/https/m3u8)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (canSave) onConfirm(name, url) }, enabled = canSave) {
                Text("保存并播放", color = PlayButtonColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun SettingsDialog(
    versionName: String,
    onRefreshLocal: () -> Unit,
    onCheckUpdate: () -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        title = { Text("设置", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsRow("重新拉取本地电台", onRefreshLocal)
                SettingsRow("检查新版本", onCheckUpdate)
                SettingsRow("关于 LX FM  v$versionName") { }
                SettingsRow("退出应用", onExit)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = PlayButtonColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SettingsRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, color = TextPrimary, fontSize = 16.sp)
    }
}

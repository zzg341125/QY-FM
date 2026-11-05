package com.dragon.fm.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NavigateBefore
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dragon.fm.MainActivity
import com.dragon.fm.R
import com.dragon.fm.data.model.PlaybackStatus
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.ui.theme.ThemePreset
import com.dragon.fm.ui.viewmodel.MainViewModel
import java.io.File
import kotlin.math.abs

/**
 * 极简版主界面：电台播放列表 + 添加电台 + 设置入口 + 底部正在播放条。
 */
@Composable
fun SimpleHomeScreen(viewModel: MainViewModel) {
    val playlist by viewModel.playlist.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val themeId by viewModel.simpleThemeId.collectAsState()
    val transparentUi by viewModel.transparentUi.collectAsState()
    val clickStationToPlay by viewModel.clickStationToPlay.collectAsState()
    val editStationEnabled by viewModel.editStationEnabled.collectAsState()
    val editStreamUrlEnabled by viewModel.editStreamUrlEnabled.collectAsState()
    val iconVersion by viewModel.iconRefreshVersion.collectAsState()
    val sleepRemaining by viewModel.sleepTimerSeconds.collectAsState()
    val selectedSleepMinutes by viewModel.selectedSleepMinutes.collectAsState()

    val preset = remember(themeId) { ThemePreset.fromId(themeId) }
    val colors = remember(preset) { simpleColors(preset) }
    ApplySystemBarAppearance(preset.isDark)

    val context = LocalContext.current
    val listState = rememberLazyListState()

    var showSettings by remember { mutableStateOf(false) }
    var showAddStation by remember { mutableStateOf(false) }
    var expandedPlayer by remember { mutableStateOf(false) }
    var editingStation by remember { mutableStateOf<RadioStation?>(null) }
    var removingStation by remember { mutableStateOf<RadioStation?>(null) }

    // 拖动排序状态
    var draggingStationId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 100.dp.toPx() }

    // 设置页内按返回键先回到主列表
    BackHandler(enabled = showSettings) { showSettings = false }

    // 从后台回到前台时，若之前是暂停状态则自动续播
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.resumeIfPaused()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showSettings) {
        SimpleSettingsScreen(viewModel = viewModel, onBack = { showSettings = false })
        return
    }

    val currentStation = playerState.currentStation
    val currentId = currentStation?.id

    // 长按底部播放条：15 → 30 → 45 → 60 → 关闭 循环切换
    fun cycleSleepTimer() {
        val next = when (selectedSleepMinutes) {
            15 -> 30
            30 -> 45
            45 -> 60
            60 -> 0
            else -> 15
        }
        viewModel.setSleepTimer(next)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState
        ) {
            if (playlist.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 120.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "播放列表为空",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "点击下方「添加新电台」，或到设置中导入 M3U 播放列表",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            itemsIndexed(playlist, key = { _, station -> station.id }) { index, station ->
                val isEditing = editingStation?.id == station.id
                val isDragging = draggingStationId == station.id
                Column(
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffset else 0f
                        }
                ) {
                    HomeStationRow(
                        station = station,
                        isCurrent = station.id == currentId,
                        isPlaying = station.id == currentId && playerState.status == PlaybackStatus.PLAYING,
                        colors = colors,
                        iconVersion = iconVersion,
                        clickToPlay = clickStationToPlay,
                        onPlay = {
                            if (station.id == currentId) viewModel.togglePlayPause()
                            else viewModel.playStationAtIndex(index)
                        },
                        onLongPress = {
                            if (editStationEnabled) {
                                editingStation = if (isEditing) null else station
                            }
                        },
                        onRequestDelete = { removingStation = station },
                        onDragStart = {
                            draggingStationId = station.id
                            dragOffset = 0f
                        },
                        onDrag = { dy ->
                            dragOffset += dy
                            val current = viewModel.playlist.value
                            val from = current.indexOfFirst { it.id == station.id }
                            if (from >= 0 && abs(dragOffset) >= rowHeightPx) {
                                val dir = if (dragOffset > 0f) 1 else -1
                                val to = (from + dir).coerceIn(0, current.lastIndex)
                                if (to != from) {
                                    viewModel.moveStation(from, to)
                                    dragOffset -= dir * rowHeightPx
                                }
                            }
                        },
                        onDragEnd = {
                            draggingStationId = null
                            dragOffset = 0f
                        }
                    )
                    if (isEditing) {
                        StationInlineEditor(
                            station = station,
                            allowUrlEdit = editStreamUrlEnabled,
                            colors = colors,
                            iconVersion = iconVersion,
                            onCancel = { editingStation = null },
                            onSave = { name, url ->
                                viewModel.updateStationInfo(station.id, name, url)
                                editingStation = null
                            },
                            onLogoPicked = { uri -> viewModel.updateStationLogo(station.id, uri) },
                            onAddToHome = { pinStationShortcut(context, station) }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.textSecondary.copy(alpha = 0.30f))
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomePillButton(
                        text = "添加新电台",
                        icon = { tint ->
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "添加新电台",
                                tint = tint,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = colors,
                        modifier = Modifier.weight(1.5f),
                        onClick = { showAddStation = true }
                    )
                    HomePillButton(
                        text = "设置",
                        icon = { tint ->
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "设置",
                                tint = tint,
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        colors = colors,
                        modifier = Modifier.weight(0.8f),
                        onClick = { showSettings = true }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (transparentUi) colors.bottomBar.copy(alpha = 0.88f) else colors.bottomBar)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val nowStation = playlist.firstOrNull { it.id == currentStation?.id } ?: currentStation
            val nowTitle = playerState.nowPlayingTitle.ifBlank { currentStation?.currentProgram.orEmpty() }
            val nowIsPlaying = playerState.status == PlaybackStatus.PLAYING

            val sleepLabel = if (sleepRemaining != null && (selectedSleepMinutes ?: 0) > 0) {
                "${selectedSleepMinutes} 分"
            } else null

            if (expandedPlayer) {
                ExpandedSleepCard(
                    station = nowStation,
                    title = nowTitle,
                    colors = colors,
                    sleepLabel = sleepLabel,
                    onCycleSleep = { cycleSleepTimer() },
                    onCollapse = { expandedPlayer = false }
                )
            } else {
                SimpleNowPlayingBar(
                    station = nowStation,
                    title = nowTitle,
                    isPlaying = nowIsPlaying,
                    colors = colors,
                    transparent = transparentUi,
                    sleepRemaining = sleepRemaining,
                    sleepLabel = null,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onLongPress = { expandedPlayer = true }
                )
            }
        }
    }

    if (showAddStation) {
        HomeAddStationDialog(
            colors = colors,
            onDismiss = { showAddStation = false },
            onConfirm = { name, url ->
                viewModel.addCustomStation(name, url)
                showAddStation = false
            }
        )
    }

    removingStation?.let { station ->
        RemoveStationDialog(
            station = station,
            colors = colors,
            onDismiss = { removingStation = null },
            onConfirm = {
                viewModel.removeStationFromPlaylist(station.id)
                removingStation = null
            }
        )
    }
}

@Composable
private fun HomeStationRow(
    station: RadioStation,
    isCurrent: Boolean,
    isPlaying: Boolean,
    colors: SimpleColors,
    iconVersion: Int,
    clickToPlay: Boolean,
    onPlay: () -> Unit,
    onLongPress: () -> Unit,
    onRequestDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val context = LocalContext.current
    val iconModel = remember(station.favicon) { stationIconModel(station.favicon) }
    val iconRequest = remember(iconModel, iconVersion) {
        ImageRequest.Builder(context)
            .data(iconModel)
            .memoryCacheKey("${station.favicon}#$iconVersion")
            .diskCacheKey("${station.favicon}#$iconVersion")
            .crossfade(true)
            .build()
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onRequestDelete()
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE9B7B3)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.RemoveCircle,
                    contentDescription = "删除电台",
                    tint = Color(0xFFB3261E),
                    modifier = Modifier
                        .padding(end = 28.dp)
                        .size(26.dp)
                )
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(colors.background)
                .pointerInput(station.id, clickToPlay) {
                    // 播放按钮区域（行尾约 76dp）交给按钮自身处理，避免点击重复触发
                    val buttonZoneStart = size.width - 76.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (down.position.x >= buttonZoneStart) return@awaitEachGesture
                        val longPress = awaitLongPressOrCancellation(down.id)
                        if (longPress == null) {
                            val stillPressed = currentEvent.changes
                                .any { it.id == down.id && it.pressed }
                            if (!stillPressed && clickToPlay) onPlay()
                            return@awaitEachGesture
                        }
                        // 长按后：原地松手 = 编辑；移动 = 拖动排序
                        var dragging = false
                        var accumulated = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (dragging) onDragEnd() else onLongPress()
                                break
                            }
                            val dy = change.position.y - change.previousPosition.y
                            if (!dragging) {
                                accumulated += dy
                                if (abs(accumulated) > viewConfiguration.touchSlop) {
                                    dragging = true
                                    onDragStart()
                                    change.consume()
                                }
                            } else if (dy != 0f) {
                                onDrag(dy)
                                change.consume()
                            }
                        }
                    }
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = iconRequest,
                contentDescription = station.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (colors.isDark) Color(0xFF2A2C31) else Color(0xFFEFEFEF))
            )
            Spacer(modifier = Modifier.width(18.dp))
            Text(
                text = station.name,
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(1.2.dp, if (isCurrent) colors.playButton else colors.outline, CircleShape)
                    .combinedClickable(
                        onClick = onPlay,
                        onLongClick = onLongPress
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isCurrent && isPlaying -> Icons.Filled.Stop
                        else -> Icons.Filled.PlayArrow
                    },
                    contentDescription = if (isCurrent && isPlaying) "停止" else "播放",
                    tint = if (isCurrent) colors.playButton else colors.outline,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun HomePillButton(
    text: String,
    icon: @Composable (Color) -> Unit,
    colors: SimpleColors,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (highlight) colors.playButton else colors.textPrimary
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = when {
            highlight -> colors.playButton.copy(alpha = if (colors.isDark) 0.22f else 0.12f)
            colors.isDark -> Color(0xFF24262B)
            else -> Color(0xFFF0F1F4)
        },
        border = BorderStroke(
            1.dp,
            if (highlight) colors.playButton.copy(alpha = 0.7f) else colors.divider
        ),
        modifier = modifier.height(52.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon(contentColor)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/** 底部“正在播放”条，主界面与设置页共用 */
@Composable
internal fun SimpleNowPlayingBar(
    station: RadioStation?,
    title: String,
    isPlaying: Boolean,
    colors: SimpleColors,
    transparent: Boolean = false,
    sleepRemaining: Int? = null,
    onTogglePlay: () -> Unit,
    sleepLabel: String? = null,
    onLongPress: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val iconModel = remember(station?.favicon) { stationIconModel(station?.favicon) }
    val iconRequest = remember(iconModel) {
        ImageRequest.Builder(context)
            .data(iconModel)
            .crossfade(true)
            .build()
    }

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = if (transparent) colors.nowPlayingBg.copy(alpha = 0.82f) else colors.nowPlayingBg,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onTogglePlay,
                    onLongClick = { onLongPress?.invoke() }
                )
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = iconRequest,
                contentDescription = station?.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(if (colors.isDark) Color(0xFF2A2C31) else Color.White)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title.ifBlank { station?.name ?: "暂未选择电台" },
                        color = colors.playButton,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (sleepRemaining != null && sleepLabel == null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.Bedtime,
                            contentDescription = "睡眠定时",
                            tint = colors.playButton,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%02d:%02d", sleepRemaining / 60, sleepRemaining % 60),
                            color = colors.playButton,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = station?.name ?: "点击列表开始收听",
                    color = colors.playButton.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!sleepLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Bedtime,
                    contentDescription = "睡眠定时",
                    tint = colors.playButton,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = sleepLabel,
                    color = colors.playButton,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.playButton)
                    .combinedClickable(
                        onClick = onTogglePlay,
                        onLongClick = { onLongPress?.invoke() }
                    ),
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

private fun stationIconModel(favicon: String?): Any? {
    if (favicon.isNullOrBlank()) return null
    return if (favicon.startsWith("/")) File(favicon) else favicon
}

@Composable
private fun simpleFieldColors(colors: SimpleColors) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = colors.textPrimary,
    unfocusedTextColor = colors.textPrimary,
    focusedLabelColor = colors.playButton,
    unfocusedLabelColor = colors.textSecondary,
    cursorColor = colors.playButton,
    focusedBorderColor = colors.playButton,
    unfocusedBorderColor = colors.divider,
    focusedContainerColor = colors.fieldBg,
    unfocusedContainerColor = colors.fieldBg
)

@Composable
private fun HomeAddStationDialog(
    colors: SimpleColors,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val canSave = name.isNotBlank() && url.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.fieldBg,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.Radio, contentDescription = null, tint = colors.playButton) },
        title = { Text("添加新电台", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("电台名称") },
                    singleLine = true,
                    colors = simpleFieldColors(colors),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("直播流地址 (http/https/m3u8)") },
                    singleLine = true,
                    colors = simpleFieldColors(colors),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (canSave) onConfirm(name, url) }, enabled = canSave) {
                Text("保存并播放", color = colors.playButton, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = colors.textSecondary)
            }
        }
    )
}

@Composable
private fun RemoveStationDialog(
    station: RadioStation,
    colors: SimpleColors,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.fieldBg,
        shape = RoundedCornerShape(22.dp),
        title = { Text("移除此电台吗？", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "- ${station.name}",
                color = colors.textPrimary,
                fontSize = 16.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("移除", color = Color(0xFFD6473B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = colors.textSecondary)
            }
        }
    )
}

@Composable
private fun SleepTimerDialog(
    colors: SimpleColors,
    remainingSeconds: Int?,
    selectedMinutes: Int?,
    onEditStation: (() -> Unit)? = null,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.fieldBg,
        shape = RoundedCornerShape(22.dp),
        title = {
            Column {
                Text("睡眠定时", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                if (remainingSeconds != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "剩余 " + String.format(
                            "%02d:%02d",
                            remainingSeconds / 60,
                            remainingSeconds % 60
                        ),
                        color = colors.playButton,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (onEditStation != null) {
                    SleepOptionRow(
                        text = "编辑此电台",
                        selected = false,
                        colors = colors,
                        onClick = onEditStation
                    )
                }
                if (remainingSeconds != null) {
                    SleepOptionRow(
                        text = "关闭定时",
                        selected = false,
                        colors = colors,
                        onClick = { onSelect(0) }
                    )
                }
                listOf(15, 30, 45, 60).forEach { minutes ->
                    SleepOptionRow(
                        text = "$minutes 分钟",
                        selected = selectedMinutes == minutes,
                        colors = colors,
                        onClick = { onSelect(minutes) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = colors.textSecondary)
            }
        }
    )
}

@Composable
private fun SleepOptionRow(
    text: String,
    selected: Boolean,
    colors: SimpleColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (selected) colors.playButton else colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colors.playButton,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** 长按电台后展开的行内编辑（名称 / 流媒体链接 / 台标） */
@Composable
private fun StationInlineEditor(
    station: RadioStation,
    allowUrlEdit: Boolean,
    colors: SimpleColors,
    iconVersion: Int,
    onCancel: () -> Unit,
    onSave: (String, String) -> Unit,
    onLogoPicked: (Uri) -> Unit,
    onAddToHome: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(station.id) { mutableStateOf(station.name) }
    var url by remember(station.id) { mutableStateOf(station.playUrl) }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) onLogoPicked(uri)
    }
    val iconModel = remember(station.favicon) { stationIconModel(station.favicon) }
    val iconRequest = remember(iconModel, iconVersion) {
        ImageRequest.Builder(context)
            .data(iconModel)
            .memoryCacheKey("${station.favicon}#$iconVersion")
            .diskCacheKey("${station.favicon}#$iconVersion")
            .crossfade(true)
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.divider.copy(alpha = 0.35f))
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = iconRequest,
                contentDescription = station.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (colors.isDark) Color(0xFF2A2C31) else Color(0xFFDDDDDD))
                    .clickable { logoPicker.launch("image/*") }
            )
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                UnderlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                    colors = colors
                )
                if (allowUrlEdit) {
                    Spacer(modifier = Modifier.height(12.dp))
                    UnderlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        textStyle = TextStyle(fontSize = 16.sp),
                        colors = colors
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { logoPicker.launch("image/*") },
                shape = CircleShape,
                color = colors.background,
                border = BorderStroke(1.dp, colors.divider),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "更换台标", color = colors.textPrimary, fontSize = 15.sp)
                }
            }
            Surface(
                onClick = onAddToHome,
                shape = CircleShape,
                color = colors.background,
                border = BorderStroke(1.dp, colors.divider),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "添加到桌面", color = colors.textPrimary, fontSize = 15.sp)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("取消", color = colors.textSecondary, fontWeight = FontWeight.Medium)
            }
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, if (allowUrlEdit) url else station.playUrl)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("保存更改", color = colors.playButton, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun UnderlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    colors: SimpleColors
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.playButton),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(3.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.2.dp)
            .background(colors.textPrimary.copy(alpha = 0.45f))
    )
}

/** 长按底部播放条展开：只保留睡眠定时，大按钮循环切换 15→30→45→60→关闭 */
@Composable
private fun ExpandedSleepCard(
    station: RadioStation?,
    title: String,
    colors: SimpleColors,
    sleepLabel: String?,
    onCycleSleep: () -> Unit,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val iconModel = remember(station?.favicon) { stationIconModel(station?.favicon) }
    val iconRequest = remember(iconModel) {
        ImageRequest.Builder(context)
            .data(iconModel)
            .crossfade(true)
            .build()
    }
    val stationName = station?.name ?: "暂未选择电台"
    val displayTitle = title.ifBlank { stationName }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = colors.nowPlayingBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCollapse)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = iconRequest,
                    contentDescription = stationName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0xFF2A2C31) else Color.White)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        color = colors.playButton,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stationName,
                        color = colors.playButton.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(colors.playButton)
                    .clickable(onClick = onCycleSleep),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bedtime,
                    contentDescription = "睡眠定时",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = sleepLabel ?: "睡眠定时",
                color = colors.playButton,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "点击按钮切换：15 分 → 30 分 → 45 分 → 60 分 → 关闭",
                color = colors.playButton.copy(alpha = 0.75f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** 点击底部播放条后展开的播放卡片 */
@Composable
private fun ExpandedNowPlayingCard(
    station: RadioStation?,
    title: String,
    isPlaying: Boolean,
    colors: SimpleColors,
    sleepRemaining: Int?,
    onCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSleepClick: () -> Unit,
    onCancelSleep: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val iconModel = remember(station?.favicon) { stationIconModel(station?.favicon) }
    val iconRequest = remember(iconModel) {
        ImageRequest.Builder(context)
            .data(iconModel)
            .crossfade(true)
            .build()
    }
    val stationName = station?.name ?: "暂未选择电台"
    val displayTitle = title.ifBlank { stationName }
    val streamUrl = station?.playUrl.orEmpty()

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = colors.nowPlayingBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = iconRequest,
                    contentDescription = stationName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0xFF2A2C31) else Color.White)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onCollapse)
                ) {
                    Text(
                        text = displayTitle,
                        color = colors.playButton,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stationName,
                        color = colors.playButton.copy(alpha = 0.85f),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colors.playButton)
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "停止" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "流媒体链接",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = colors.playButton,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = streamUrl.ifBlank { "—" },
                    color = colors.playButton,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (streamUrl.isNotBlank()) {
                            clipboard.setText(AnnotatedString(streamUrl))
                            Toast.makeText(context, "已复制流媒体链接", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "复制链接",
                        tint = colors.playButton.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "正在播放",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.NavigateBefore,
                        contentDescription = "上一个电台",
                        tint = colors.playButton,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = displayTitle,
                    color = colors.playButton,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.NavigateNext,
                        contentDescription = "下一个电台",
                        tint = colors.playButton,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                if (sleepRemaining != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onCancelSleep)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✕",
                            color = colors.playButton,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%02d:%02d", sleepRemaining / 60, sleepRemaining % 60),
                            color = colors.playButton,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                IconButton(onClick = onSleepClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Bedtime,
                        contentDescription = "睡眠定时",
                        tint = colors.playButton.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** 为电台创建桌面快捷方式，点击后直接播放该电台 */
private fun pinStationShortcut(context: Context, station: RadioStation) {
    val intent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra(MainActivity.EXTRA_PLAY_STATION_ID, station.id)
    }
    val shortcut = ShortcutInfoCompat.Builder(context, "qyfm_station_${station.id}")
        .setShortLabel(station.name.take(8))
        .setLongLabel(station.name)
        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(intent)
        .build()
    val message = try {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
            "已请求添加到桌面，请在弹出的提示中确认"
        } else {
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
            "桌面不支持固定快捷方式，已加入长按应用图标的快捷方式"
        }
    } catch (e: Exception) {
        "添加失败：${e.message ?: "未知错误"}"
    }
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

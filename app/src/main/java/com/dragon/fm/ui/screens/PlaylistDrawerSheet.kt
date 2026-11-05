package com.dragon.fm.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonTheme
import com.dragon.fm.ui.theme.LiveRed
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Modern Portrait Playlist Bottom Sheet with swipe-left-to-delete and long-press-to-reorder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDrawerSheet(
    playlist: List<RadioStation>,
    currentIndex: Int,
    isPlaying: Boolean,
    selectedScenario: String,
    latencies: Map<String, Long>,
    isSpeedTesting: Boolean = false,
    onSelectStation: (Int) -> Unit,
    onRunSpeedTest: () -> Unit = {},
    onSortByLatency: () -> Unit = {},
    onRemoveStation: (String) -> Unit = {},
    onMoveStation: (Int, Int) -> Unit = { _, _ -> },
    onReorderPlaylist: ((List<RadioStation>) -> Unit)? = null,
    onAddToScenario: (RadioStation) -> Unit = {},
    fontScale: Float = 1.0f,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.78f).dp
    var isEditing by remember { mutableStateOf(false) }

    var localPlaylist by remember(playlist) { mutableStateOf(playlist) }
    var revealedDeleteStationId by remember { mutableStateOf<String?>(null) }
    var draggingStationId by remember { mutableStateOf<String?>(null) }
    var dragAccumulatedY by remember { mutableStateOf(0f) }

    val currentDensity = LocalDensity.current
    val customDensity = remember(currentDensity, fontScale) {
        androidx.compose.ui.unit.Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * fontScale
        )
    }
    val itemHeightPx = with(customDensity) { 68.dp.toPx() }

    // Smoothly scroll to current playing item when sheet opens
    LaunchedEffect(Unit) {
        if (currentIndex in playlist.indices) {
            val target = (currentIndex - 2).coerceAtLeast(0)
            listState.scrollToItem(target)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DragonTheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(DragonTheme.textSecondary.copy(alpha = 0.25f))
            )
        }
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalDensity provides customDensity
        ) {
            Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEditing) "管理播放列表" else "播放列表",
                            color = DragonTheme.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DragonTheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, DragonTheme.primary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "${playlist.size}台",
                                color = DragonTheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    val cleanScenario = selectedScenario.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
                    Text(
                        text = if (isEditing) "长按拖拽调序 · 左滑删除" else "模式 • $cleanScenario",
                        color = if (isEditing) DragonCyan else DragonTheme.textSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Edit Mode Toggle Button
                    Surface(
                        onClick = { isEditing = !isEditing },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isEditing) DragonCyan.copy(alpha = 0.18f) else DragonTheme.glassSurface,
                        border = BorderStroke(1.dp, if (isEditing) DragonCyan else DragonTheme.glassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Outlined.Check else Icons.Outlined.Edit,
                                contentDescription = if (isEditing) "Done" else "Edit",
                                tint = if (isEditing) DragonCyan else DragonTheme.textPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEditing) "完成" else "编辑",
                                color = if (isEditing) DragonCyan else DragonTheme.textPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (!isEditing) {
                        Spacer(modifier = Modifier.width(6.dp))
                        // Quick Latency Test & Sort Button
                        Surface(
                            onClick = {
                                if (!isSpeedTesting) {
                                    onRunSpeedTest()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSpeedTesting) DragonTheme.primary.copy(alpha = 0.16f) else DragonTheme.glassSurface,
                            border = BorderStroke(1.dp, if (isSpeedTesting) DragonTheme.primary else DragonTheme.glassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSpeedTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(13.dp),
                                        strokeWidth = 1.8.dp,
                                        color = DragonTheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "测速中...",
                                        color = DragonTheme.primary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Speed,
                                        contentDescription = "Speed Test",
                                        tint = if (latencies.isNotEmpty()) DragonTheme.primary else DragonTheme.textPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (latencies.isEmpty()) "测速" else "重新测速",
                                        color = if (latencies.isNotEmpty()) DragonTheme.primary else DragonTheme.textPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = DragonTheme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (playlist.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Radio,
                            contentDescription = null,
                            tint = DragonTheme.textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "当前场景暂无收录电台",
                            color = DragonTheme.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "可在探索搜索页面或播放页面将喜欢的电台收藏到本场景",
                            color = DragonTheme.textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Station Items List with Swipe-to-delete and Long-press-to-reorder
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(localPlaylist, key = { _, station -> station.id }) { index, station ->
                        val isCurrent = station.id == playlist.getOrNull(currentIndex)?.id
                        val isItemPlaying = isCurrent && isPlaying
                        val latency = latencies[station.id]
                        val isRevealed = revealedDeleteStationId == station.id
                        val isDragging = draggingStationId == station.id

                        SwipeablePlaylistItem(
                            index = index,
                            station = station,
                            isCurrent = isCurrent,
                            isItemPlaying = isItemPlaying,
                            latency = latency,
                            isEditing = isEditing,
                            isRevealed = isRevealed,
                            isDragging = isDragging,
                            dragOffsetY = if (isDragging) dragAccumulatedY else 0f,
                            onReveal = { revealedDeleteStationId = station.id },
                            onCollapse = { if (isRevealed) revealedDeleteStationId = null },
                            onSelect = {
                                val originalIndex = playlist.indexOfFirst { it.id == station.id }
                                if (originalIndex != -1) {
                                    onSelectStation(originalIndex)
                                }
                            },
                            onDelete = {
                                revealedDeleteStationId = null
                                onRemoveStation(station.id)
                            },
                            onAddToScenario = { onAddToScenario(station) },
                            onDragStart = {
                                revealedDeleteStationId = null
                                draggingStationId = station.id
                                dragAccumulatedY = 0f
                            },
                            onDrag = { deltaY ->
                                dragAccumulatedY += deltaY
                                val curIdx = localPlaylist.indexOfFirst { it.id == station.id }
                                if (curIdx != -1) {
                                    val steps = (dragAccumulatedY / itemHeightPx).toInt()
                                    if (steps != 0) {
                                        val target = (curIdx + steps).coerceIn(0, localPlaylist.size - 1)
                                        if (target != curIdx) {
                                            val mutable = localPlaylist.toMutableList()
                                            val item = mutable.removeAt(curIdx)
                                            mutable.add(target, item)
                                            localPlaylist = mutable
                                            dragAccumulatedY -= (target - curIdx) * itemHeightPx
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingStationId = null
                                dragAccumulatedY = 0f
                                onReorderPlaylist?.invoke(localPlaylist)
                            }
                        )
                    }
                }
            }
        }
        }
    }
}

/**
 * Individual Station Row supporting swipe-left to reveal Delete button and long-press drag
 */
@Composable
private fun SwipeablePlaylistItem(
    index: Int,
    station: RadioStation,
    isCurrent: Boolean,
    isItemPlaying: Boolean,
    latency: Long?,
    isEditing: Boolean,
    isRevealed: Boolean,
    isDragging: Boolean,
    dragOffsetY: Float,
    onReveal: () -> Unit,
    onCollapse: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onAddToScenario: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val maxSwipePx = with(density) { 76.dp.toPx() }
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(isRevealed) {
        offsetX.animateTo(
            targetValue = if (isRevealed) -maxSwipePx else 0f,
            animationSpec = tween(durationMillis = 200)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 20f else 1f)
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = if (isDragging) 1.04f else 1f
                scaleY = if (isDragging) 1.04f else 1f
                shadowElevation = if (isDragging) 24f else 0f
            }
    ) {
        // Red Background for Delete Action (Exposed when swiping left)
        if (offsetX.value < -2f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LiveRed)
                    .clickable { onDelete() },
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(76.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "删除",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "删除",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Foreground Card Surface
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isCurrent) DragonTheme.primary.copy(alpha = 0.12f) else DragonTheme.glassSurface,
            border = BorderStroke(
                width = if (isDragging) 1.5.dp else 1.dp,
                color = when {
                    isDragging -> DragonTheme.primary
                    isCurrent -> DragonTheme.primary.copy(alpha = 0.7f)
                    else -> DragonTheme.glassBorder
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(station.id) {
                    detectHorizontalDragGestures(
                        onDragStart = { },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val next = (offsetX.value + dragAmount).coerceIn(-maxSwipePx, 0f)
                                offsetX.snapTo(next)
                            }
                        },
                        onDragEnd = {
                            if (offsetX.value < -maxSwipePx / 2f) {
                                onReveal()
                            } else {
                                onCollapse()
                                coroutineScope.launch { offsetX.animateTo(0f) }
                            }
                        },
                        onDragCancel = {
                            onCollapse()
                            coroutineScope.launch { offsetX.animateTo(0f) }
                        }
                    )
                }
                .pointerInput(station.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.y)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    )
                }
                .clickable {
                    if (isRevealed) {
                        onCollapse()
                    } else if (!isEditing) {
                        onSelect()
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing) {
                    // Index indicator
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DragonTheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                color = DragonTheme.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // Cover Icon or Logo
                Box(
                    modifier = Modifier
                        .size(42.dp)
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
                            imageVector = Icons.Outlined.Radio,
                            contentDescription = "Station",
                            tint = if (isCurrent) DragonTheme.primary else DragonTheme.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        color = if (isCurrent) DragonTheme.primary else DragonTheme.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    val region = station.state?.takeIf { it.isNotBlank() } ?: station.country
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
                        val desc = station.currentProgram ?: (if (station.tags.isNotBlank()) "$region · ${station.tags.take(16)}" else region)
                        Text(
                            text = desc,
                            color = if (station.currentProgram != null) DragonTheme.primary.copy(alpha = 0.85f) else DragonTheme.textSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (latency != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    latency < 0 -> LiveRed.copy(alpha = 0.16f)
                                    latency < 300 -> DragonTheme.primary.copy(alpha = 0.16f)
                                    latency < 800 -> DragonCyan.copy(alpha = 0.16f)
                                    else -> DragonTheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = when {
                                        latency < 0 -> "离线"
                                        else -> "${latency}ms"
                                    },
                                    color = when {
                                        latency < 0 -> LiveRed
                                        latency < 300 -> DragonTheme.primary
                                        latency < 800 -> DragonCyan
                                        else -> DragonTheme.textSecondary
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                if (isEditing) {
                    // Modern Drag Handle
                    Icon(
                        imageVector = Icons.Outlined.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = DragonTheme.primary.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(26.dp)
                            .padding(4.dp)
                    )
                } else {
                    // Right Status Indicator & Quick Add to Scenario
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onAddToScenario,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkAdd,
                                contentDescription = "Add to Scenario",
                                tint = DragonTheme.textSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(4.dp))
                            AudioEqualizerWave(
                                isPlaying = isItemPlaying,
                                color = DragonTheme.primary,
                                modifier = Modifier.height(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.dragon.fm.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.dragon.fm.data.model.AvailableModernIcons
import com.dragon.fm.data.model.CustomScenarioMode
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.data.model.resolveScenarioIcon
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonGold
import com.dragon.fm.ui.theme.DragonTheme
import com.dragon.fm.ui.theme.LiveRed
import com.dragon.fm.ui.theme.ThemePreset

data class DrawerModeItem(
    val id: String,
    val title: String,
    val description: String = "",
    val iconEmoji: String? = null,
    val iconVector: ImageVector? = null,
    val scenarioKey: String,
    val isCustom: Boolean = false,
    val isFavoriteMode: Boolean = false,
    val stationCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StudioDrawerSheet(
    station: RadioStation?,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    isRecording: Boolean,
    recordingDurationSec: Int,
    sleepTimerMinutes: Int?,
    selectedScenario: String = "精选推荐",
    customScenarios: List<CustomScenarioMode> = emptyList(),
    deletedDefaultScenarios: Set<String> = emptySet(),
    scenarioOrder: List<String> = emptyList(),
    favoriteCount: Int = 0,
    currentTheme: ThemePreset = ThemePreset.SODA_OBSIDIAN,
    fontScale: Float = 1.0f,
    onDismiss: () -> Unit,
    onSelectScenario: (String) -> Unit = {},
    onDeleteCustomScenario: (String) -> Unit = {},
    onDeleteDefaultScenario: (String) -> Unit = {},
    onRestoreDefaultScenarios: () -> Unit = {},
    onUpdateCustomScenario: (id: String, newTitle: String, newIconKey: String) -> Unit = { _, _, _ -> },
    onReorderCustomScenarios: (List<CustomScenarioMode>) -> Unit = {},
    onReorderAllScenarios: (List<String>) -> Unit = {},
    onCreateCustomScenario: (title: String, iconKey: String) -> Unit = { _, _ -> },
    onTriggerSpeedSort: () -> Unit = {},
    onSelectTheme: (ThemePreset) -> Unit = {},
    onToggleRecording: () -> Unit,
    onSetSleepTimer: (Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showScenarioManager by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var customScenarioToEdit by remember { mutableStateOf<CustomScenarioMode?>(null) }
    var modeToDelete by remember { mutableStateOf<DrawerModeItem?>(null) }
    var quickActionItem by remember { mutableStateOf<DrawerModeItem?>(null) }

    val defaultBuiltInList = listOf(
        DrawerModeItem("builtin_featured", "精选推荐", description = "全国高分旗舰电台合集", iconEmoji = "✨", iconVector = Icons.Outlined.AutoAwesome, scenarioKey = "精选推荐"),
        DrawerModeItem("builtin_commute", "潮流通勤", description = "轻快节奏与最新热门金曲", iconEmoji = "🎵", iconVector = Icons.Outlined.MusicNote, scenarioKey = "潮流通勤"),
        DrawerModeItem("builtin_traffic", "路况交通", description = "实时拥堵与应急广播导航", iconEmoji = "🚗", iconVector = Icons.Outlined.DirectionsCar, scenarioKey = "路况交通"),
        DrawerModeItem("builtin_sleep", "晚间助眠", description = "纯音乐与深夜温情慢播", iconEmoji = "🌙", iconVector = Icons.Outlined.Bedtime, scenarioKey = "晚间助眠"),
        DrawerModeItem("builtin_news", "实时新闻", description = "国家之声与全球焦点资讯", iconEmoji = "🎙", iconVector = Icons.Outlined.Mic, scenarioKey = "实时新闻"),
        DrawerModeItem("builtin_focus", "咖啡专注", description = "爵士白噪与Lo-Fi轻音乐", iconEmoji = "☕", iconVector = Icons.Outlined.LocalCafe, scenarioKey = "咖啡专注"),
        DrawerModeItem("builtin_classic", "华语经典", description = "岁月金曲与怀旧老歌回响", iconEmoji = "📻", iconVector = Icons.Outlined.MusicNote, scenarioKey = "华语经典"),
        DrawerModeItem("builtin_rock", "摇滚动感", description = "摇滚硬核与潮流电音节奏", iconEmoji = "⚡", iconVector = Icons.Outlined.Speed, scenarioKey = "摇滚流行"),
        DrawerModeItem("builtin_world", "环球视界", description = "跨越国界的多语言国际广播", iconEmoji = "🌐", iconVector = Icons.Outlined.Public, scenarioKey = "环球视界"),
        DrawerModeItem("builtin_local", "本地广播", description = "附近省市同城广播向导", iconEmoji = "📍", iconVector = Icons.Outlined.NearMe, scenarioKey = "本地电台"),
        DrawerModeItem("builtin_roam", "随机漫游", description = "随心跳动发现未听过电波", iconEmoji = "🎲", iconVector = Icons.Outlined.Casino, scenarioKey = "随机漫游")
    ).filter { it.scenarioKey !in deletedDefaultScenarios && !deletedDefaultScenarios.any { d -> d.contains(it.title) } }

    val favoriteModeItem = if (favoriteCount > 0) {
        listOf(DrawerModeItem("fav_mode", "我的收藏", description = "心选电台，专属私藏音频流", iconEmoji = "💖", iconVector = Icons.Outlined.Star, scenarioKey = "我的收藏", isFavoriteMode = true, stationCount = favoriteCount))
    } else emptyList()

    val cleanCustomScenarios = customScenarios.filterNot {
        it.id.contains("canton", ignoreCase = true) ||
        it.title.contains("canton", ignoreCase = true) ||
        it.title.contains("粤语", ignoreCase = true)
    }

    val customModeItems = cleanCustomScenarios.map { c ->
        DrawerModeItem(
            id = c.id,
            title = c.title,
            description = "自定义专属个性场景",
            iconEmoji = c.iconEmoji ?: "📻",
            scenarioKey = c.title,
            isCustom = true,
            stationCount = c.stations.size
        )
    }

    val unsortedModes = favoriteModeItem + customModeItems + defaultBuiltInList
    val allModes = if (scenarioOrder.isNotEmpty()) {
        unsortedModes.sortedBy { item ->
            val idx = scenarioOrder.indexOf(item.scenarioKey)
            if (idx >= 0) idx else 999
        }
    } else {
        unsortedModes
    }

    var localOrderedKeys by remember(allModes) {
        mutableStateOf(allModes.map { it.scenarioKey })
    }

    val orderedAllModes = remember(localOrderedKeys, allModes) {
        val map = allModes.associateBy { it.scenarioKey }
        val list = mutableListOf<DrawerModeItem>()
        for (k in localOrderedKeys) {
            map[k]?.let { list.add(it) }
        }
        for (m in allModes) {
            if (!localOrderedKeys.contains(m.scenarioKey)) {
                list.add(m)
            }
        }
        list
    }

    var isOrganizeMode by remember { mutableStateOf(false) }
    val visibleModes = if (isExpanded || isOrganizeMode || orderedAllModes.size <= 8) orderedAllModes else orderedAllModes.take(8)

    var draggingScenarioKey by remember { mutableStateOf<String?>(null) }
    val slotBounds = remember { mutableStateMapOf<Int, Rect>() }
    var gridCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragCurrentPosition by remember { mutableStateOf(Offset.Zero) }
    var dragTouchAnchor by remember { mutableStateOf(Offset.Zero) }

    val currentDensity = LocalDensity.current
    val customDensity = remember(currentDensity, fontScale) {
        androidx.compose.ui.unit.Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * fontScale
        )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "收听模式",
                    color = DragonTheme.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isOrganizeMode) {
                    Surface(
                        onClick = { isOrganizeMode = false },
                        shape = RoundedCornerShape(10.dp),
                        color = DragonTheme.primary,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "完成整理",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // + New Scenario Button
                        Surface(
                            onClick = { showCreateDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = DragonTheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, DragonTheme.primary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "Add",
                                    tint = DragonTheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "新建场景",
                                    color = DragonTheme.primary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Manage Scenarios Button
                        Surface(
                            onClick = { showScenarioManager = true },
                            shape = RoundedCornerShape(10.dp),
                            color = DragonTheme.glassSurface,
                            border = BorderStroke(1.dp, DragonTheme.glassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DashboardCustomize,
                                    contentDescription = "Manage",
                                    tint = DragonTheme.textPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "管理场景",
                                    color = DragonTheme.textPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subheader: 场景与个性化模式库
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            tint = DragonTheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "收听场景模式 (${allModes.size})",
                            color = DragonTheme.textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isOrganizeMode) "桌面整理中：长按拖拽卡片调整顺序，点击角标编辑或删除" else "为不同时间与心情切换电波流，沉浸专属声音氛围",
                        color = if (isOrganizeMode) DragonCyan else DragonTheme.textSecondary,
                        fontSize = 11.5.sp
                    )
                }

                if (!isOrganizeMode && deletedDefaultScenarios.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRestoreDefaultScenarios() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "恢复", tint = DragonCyan, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "恢复默认", color = DragonCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2-Column Soundscape Cards Grid (仿桌面图标长按整理、动态拖拽调序、编辑与删除)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { gridCoordinates = it }
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { startOffset ->
                                val keys = localOrderedKeys
                                val maxVisible = if (isExpanded || isOrganizeMode || keys.size <= 8) keys.size else 8
                                val hitEntry = slotBounds.entries
                                    .filter { (slotIdx, rect) ->
                                        slotIdx < maxVisible &&
                                        Rect(rect.left - 24f, rect.top - 24f, rect.right + 24f, rect.bottom + 24f).contains(startOffset)
                                    }
                                    .minByOrNull { (_, rect) ->
                                        val center = rect.center
                                        val dx = center.x - startOffset.x
                                        val dy = center.y - startOffset.y
                                        dx * dx + dy * dy
                                    }
                                if (hitEntry != null) {
                                    val slotIndex = hitEntry.key
                                    val key = keys.getOrNull(slotIndex)
                                    if (key != null) {
                                        isOrganizeMode = true
                                        draggingScenarioKey = key
                                        dragTouchAnchor = startOffset - hitEntry.value.topLeft
                                        dragCurrentPosition = startOffset
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragCurrentPosition += dragAmount
                                val key = draggingScenarioKey ?: return@detectDragGesturesAfterLongPress

                                val keys = localOrderedKeys
                                val maxVisible = if (isExpanded || isOrganizeMode || keys.size <= 8) keys.size else 8
                                val curIdx = keys.indexOf(key)
                                if (curIdx != -1) {
                                    val targetEntry = slotBounds.entries
                                        .filter { (slotIdx, rect) ->
                                            slotIdx < maxVisible &&
                                            Rect(rect.left - 24f, rect.top - 24f, rect.right + 24f, rect.bottom + 24f).contains(dragCurrentPosition)
                                        }
                                        .minByOrNull { (_, rect) ->
                                            val center = rect.center
                                            val dx = center.x - dragCurrentPosition.x
                                            val dy = center.y - dragCurrentPosition.y
                                            dx * dx + dy * dy
                                        }
                                    if (targetEntry != null) {
                                        val targetSlot = targetEntry.key
                                        if (targetSlot != curIdx && targetSlot < maxVisible) {
                                            val mutable = keys.toMutableList()
                                            val moved = mutable.removeAt(curIdx)
                                            mutable.add(targetSlot, moved)
                                            localOrderedKeys = mutable
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingScenarioKey = null
                                onReorderAllScenarios(localOrderedKeys)
                            },
                            onDragCancel = {
                                draggingScenarioKey = null
                                onReorderAllScenarios(localOrderedKeys)
                            }
                        )
                    }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val gridItems = visibleModes.toMutableList()
                    gridItems.chunked(2).forEachIndexed { rowIndex, rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEachIndexed { colIndex, item ->
                                val slotIndex = rowIndex * 2 + colIndex
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { coords ->
                                            val root = gridCoordinates
                                            if (root != null && coords.isAttached && root.isAttached) {
                                                val rel = root.localPositionOf(coords, Offset.Zero)
                                                slotBounds[slotIndex] = Rect(
                                                    offset = rel,
                                                    size = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
                                                )
                                            }
                                        }
                                ) {
                                    key(item.scenarioKey) {
                                        val isSelected = selectedScenario == item.scenarioKey || (item.isCustom && selectedScenario == item.title)
                                        val isDragging = draggingScenarioKey == item.scenarioKey

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .zIndex(if (isDragging) 100f else 1f)
                                                .graphicsLayer {
                                                    if (isDragging) {
                                                        val slotTopLeft = slotBounds[slotIndex]?.topLeft ?: Offset.Zero
                                                        val desiredTopLeft = dragCurrentPosition - dragTouchAnchor
                                                        translationX = desiredTopLeft.x - slotTopLeft.x
                                                        translationY = desiredTopLeft.y - slotTopLeft.y
                                                        scaleX = 1.08f
                                                        scaleY = 1.08f
                                                        shadowElevation = 32f
                                                    }
                                                }
                                        ) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) DragonTheme.primary.copy(alpha = 0.16f) else DragonTheme.glassSurface,
                                            border = BorderStroke(
                                                width = if (isDragging) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isDragging) DragonCyan else if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (!isOrganizeMode) {
                                                        onSelectScenario(item.scenarioKey)
                                                        onDismiss()
                                                    }
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(13.dp)
                                            ) {
                                        // Top Emoji / Icon Squircle
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) DragonTheme.primary.copy(alpha = 0.22f)
                                                    else DragonTheme.surfaceVariant.copy(alpha = 0.55f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val isEmoji = !item.iconEmoji.isNullOrBlank() && item.iconEmoji.length <= 2
                                            if (isEmoji) {
                                                Text(text = item.iconEmoji!!, fontSize = 19.sp)
                                            } else {
                                                val resolvedIcon = if (!item.iconEmoji.isNullOrBlank()) {
                                                    resolveScenarioIcon(item.iconEmoji)
                                                } else {
                                                    item.iconVector ?: Icons.Outlined.Radio
                                                }
                                                Icon(
                                                    imageVector = resolvedIcon,
                                                    contentDescription = item.title,
                                                    tint = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(9.dp))

                                        // Mode Title
                                        Text(
                                            text = item.title,
                                            color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        // Soundscape Description Subtitle
                                        Text(
                                            text = item.description,
                                            color = DragonTheme.textSecondary,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(7.dp))

                                        // Status & Channel Count
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(DragonTheme.primary)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (item.stationCount > 0) "当前启用 · ${item.stationCount}频道" else "当前启用",
                                                    color = DragonTheme.primary,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            } else {
                                                Text(
                                                    text = if (item.stationCount > 0) "${item.stationCount} 频道" else "全景电波",
                                                    color = DragonTheme.textSecondary.copy(alpha = 0.70f),
                                                    fontSize = 10.5.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // Top-Right Badge Buttons in Organize Mode (类似桌面图标右上角✕与✏️)
                                if (isOrganizeMode && !isDragging) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 6.dp, end = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (item.isCustom) {
                                            // Edit Badge Button
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(DragonTheme.surfaceVariant.copy(alpha = 0.95f))
                                                    .clickable {
                                                        val match = customScenarios.find { it.id == item.id || it.title == item.title }
                                                        if (match != null) {
                                                            customScenarioToEdit = match
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Edit,
                                                    contentDescription = "Edit",
                                                    tint = DragonCyan,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }

                                            // Delete Badge Button
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(LiveRed.copy(alpha = 0.92f))
                                                    .clickable {
                                                        modeToDelete = item
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "Delete",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        } else if (!item.isFavoriteMode) {
                                            // Hide Built-in Scenario Badge Button
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(LiveRed.copy(alpha = 0.88f))
                                                    .clickable {
                                                        modeToDelete = item
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "Hide",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Pad empty column if odd count
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

    // Expand / Collapse Fold Button when exceeding 8 items (hidden in organize mode as all modes are shown)
    if (!isOrganizeMode && allModes.size > 8) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Fold",
                        tint = DragonTheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExpanded) "收起模式列表" else "展开更多模式 (${allModes.size - 8})",
                        color = DragonTheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Multi-style Theme Engine Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = DragonTheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "主题设计风格",
                        color = DragonTheme.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = currentTheme.title,
                    color = currentTheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ThemePreset.entries) { preset ->
                    val isSelected = preset == currentTheme
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) preset.surfaceVariant else (if (DragonTheme.isDark) Color(0x331E293B) else DragonTheme.glassSurface),
                        border = if (isSelected) BorderStroke(1.5.dp, preset.primary) else BorderStroke(0.5.dp, DragonTheme.glassBorder),
                        modifier = Modifier.clickable { onSelectTheme(preset) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(preset.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = preset.title,
                                color = if (isSelected) preset.primary else DragonTheme.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Extra Controls: Recording & Sleep Timer
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DragonTheme.glassSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Recording Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FiberManualRecord,
                                contentDescription = "Rec",
                                tint = if (isRecording) LiveRed else DragonTheme.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRecording) "正在录制 (${recordingDurationSec}s)" else "实时空中录音",
                                color = DragonTheme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = onToggleRecording,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) LiveRed else DragonCyan,
                                contentColor = if (isRecording) Color.White else Color(0xFF0F172A)
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(text = if (isRecording) "结束录制" else "开始录制", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sleep Timer row (Fixed: uses selectedSleepMinutes directly)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Bedtime,
                                contentDescription = "Sleep",
                                tint = DragonGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "睡眠伴听定时",
                                color = DragonTheme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0 to "关", 15 to "15分", 30 to "30分", 60 to "60分").forEach { (mins, label) ->
                                val isCur = if (mins == 0) (sleepTimerMinutes == null || sleepTimerMinutes == 0) else sleepTimerMinutes == mins
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCur) DragonGold else DragonTheme.surfaceVariant,
                                    modifier = Modifier.clickable { onSetSleepTimer(mins) }
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isCur) Color(0xFF0F172A) else DragonTheme.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    // --- DIALOGS & SUB-PANELS ---

    // 1. Scenario Management Dialog
    if (showScenarioManager) {
        val configuration = LocalConfiguration.current
        val dialogMaxHeight = (configuration.screenHeightDp * 0.85f).dp

        AlertDialog(
            onDismissRequest = { showScenarioManager = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "场景管理",
                        color = DragonTheme.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        onClick = {
                            showScenarioManager = false
                            showCreateDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = DragonTheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, DragonTheme.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Add, null, tint = DragonTheme.primary, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("新建", color = DragonTheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            text = {
                Column(modifier = Modifier.heightIn(max = dialogMaxHeight)) {
                    Text(
                        text = "调整场景顺序、编辑标题或矢量图标",
                        color = DragonTheme.textSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(allModes, key = { _, item -> item.id }) { index, item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DragonTheme.glassSurface,
                                border = BorderStroke(1.dp, DragonTheme.glassBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = if (!item.iconEmoji.isNullOrBlank()) {
                                        resolveScenarioIcon(item.iconEmoji)
                                    } else {
                                        item.iconVector ?: Icons.Outlined.Radio
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (item.isCustom) DragonCyan else (if (item.isFavoriteMode) DragonGold else DragonTheme.primary),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            color = DragonTheme.textPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = when {
                                                item.isFavoriteMode -> "我的收藏"
                                                item.isCustom -> "自定义场景 (${item.stationCount} 台)"
                                                else -> "内置场景"
                                            },
                                            color = DragonTheme.textSecondary,
                                            fontSize = 10.5.sp
                                        )
                                    }

                                    // Move Up
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutable = allModes.toMutableList()
                                                val moved = mutable.removeAt(index)
                                                mutable.add(index - 1, moved)
                                                onReorderAllScenarios(mutable.map { it.scenarioKey })
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.KeyboardArrowUp,
                                            "Move Up",
                                            tint = if (index > 0) DragonTheme.textPrimary else DragonTheme.textSecondary.copy(alpha = 0.2f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Move Down
                                    IconButton(
                                        onClick = {
                                            if (index < allModes.size - 1) {
                                                val mutable = allModes.toMutableList()
                                                val moved = mutable.removeAt(index)
                                                mutable.add(index + 1, moved)
                                                onReorderAllScenarios(mutable.map { it.scenarioKey })
                                            }
                                        },
                                        enabled = index < allModes.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.KeyboardArrowDown,
                                            "Move Down",
                                            tint = if (index < allModes.size - 1) DragonTheme.textPrimary else DragonTheme.textSecondary.copy(alpha = 0.2f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (item.isCustom) {
                                        // Edit Button
                                        IconButton(
                                            onClick = {
                                                val match = customScenarios.find { it.id == item.id || it.title == item.title }
                                                if (match != null) {
                                                    customScenarioToEdit = match
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Outlined.Edit, "Edit", tint = DragonCyan, modifier = Modifier.size(15.dp))
                                        }

                                        // Delete Button
                                        IconButton(
                                            onClick = { modeToDelete = item },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Outlined.Delete, "Delete", tint = LiveRed.copy(alpha = 0.85f), modifier = Modifier.size(15.dp))
                                        }
                                    } else if (!item.isFavoriteMode) {
                                        // Hide Button for built-in scenario
                                        IconButton(
                                            onClick = { modeToDelete = item },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Outlined.VisibilityOff, "Hide", tint = DragonTheme.textSecondary, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScenarioManager = false }) {
                    Text("完成", color = DragonTheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DragonTheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 2. Quick Action Modal on Card Long-Press
    quickActionItem?.let { target ->
        AlertDialog(
            onDismissRequest = { quickActionItem = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = if (!target.iconEmoji.isNullOrBlank()) {
                        resolveScenarioIcon(target.iconEmoji)
                    } else {
                        target.iconVector ?: Icons.Outlined.Radio
                    }
                    Icon(icon, null, tint = DragonTheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = "管理「${target.title}」", color = DragonTheme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (target.isCustom) {
                        Surface(
                            onClick = {
                                val match = customScenarios.find { it.id == target.id || it.title == target.title }
                                quickActionItem = null
                                if (match != null) {
                                    customScenarioToEdit = match
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = DragonTheme.glassSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Edit, null, tint = DragonCyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("编辑标题与图标", color = DragonTheme.textPrimary, fontSize = 13.5.sp)
                            }
                        }
                    }

                    // Move to Top
                    Surface(
                        onClick = {
                            val curIdx = allModes.indexOfFirst { it.id == target.id }
                            if (curIdx > 0) {
                                val mutable = allModes.toMutableList()
                                val moved = mutable.removeAt(curIdx)
                                mutable.add(0, moved)
                                onReorderAllScenarios(mutable.map { it.scenarioKey })
                            }
                            quickActionItem = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = DragonTheme.glassSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.VerticalAlignTop, null, tint = DragonTheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("移到最前", color = DragonTheme.textPrimary, fontSize = 13.5.sp)
                        }
                    }

                    if (!target.isFavoriteMode) {
                        // Delete / Hide
                        Surface(
                            onClick = {
                                quickActionItem = null
                                modeToDelete = target
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = LiveRed.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (target.isCustom) Icons.Outlined.Delete else Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    tint = LiveRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (target.isCustom) "删除此自定义场景" else "从主列表隐藏",
                                    color = LiveRed,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { quickActionItem = null }) {
                    Text("取消", color = DragonTheme.textSecondary)
                }
            },
            containerColor = DragonTheme.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // 3. Edit Custom Scenario Dialog
    customScenarioToEdit?.let { target ->
        var editTitle by remember(target.id) { mutableStateOf(target.title) }
        var editIconKey by remember(target.id) { mutableStateOf(target.iconEmoji) }

        AlertDialog(
            onDismissRequest = { customScenarioToEdit = null },
            title = {
                Text("编辑场景模式", color = DragonTheme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("场景标题", color = DragonTheme.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        singleLine = true,
                        placeholder = { Text("输入场景名称", color = DragonTheme.textSecondary.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DragonCyan,
                            unfocusedBorderColor = DragonTheme.glassBorder,
                            focusedTextColor = DragonTheme.textPrimary,
                            unfocusedTextColor = DragonTheme.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("选择极简矢量图标", color = DragonTheme.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))

                    ModernIconSelector(
                        selectedKey = editIconKey,
                        onSelectKey = { editIconKey = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = editTitle.trim()
                        if (trimmed.isNotBlank()) {
                            onUpdateCustomScenario(target.id, trimmed, editIconKey)
                            customScenarioToEdit = null
                        }
                    }
                ) {
                    Text("保存修改", color = DragonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { customScenarioToEdit = null }) {
                    Text("取消", color = DragonTheme.textSecondary)
                }
            },
            containerColor = DragonTheme.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // 4. Create New Custom Scenario Dialog
    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newIconKey by remember { mutableStateOf("sparkles") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text("新建自定义场景", color = DragonTheme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("场景标题", color = DragonTheme.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        singleLine = true,
                        placeholder = { Text("例如：深夜小馆、粤语经典", color = DragonTheme.textSecondary.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DragonCyan,
                            unfocusedBorderColor = DragonTheme.glassBorder,
                            focusedTextColor = DragonTheme.textPrimary,
                            unfocusedTextColor = DragonTheme.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("选择极简矢量图标", color = DragonTheme.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))

                    ModernIconSelector(
                        selectedKey = newIconKey,
                        onSelectKey = { newIconKey = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newTitle.trim()
                        if (trimmed.isNotBlank()) {
                            onCreateCustomScenario(trimmed, newIconKey)
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("立即创建", color = DragonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("取消", color = DragonTheme.textSecondary)
                }
            },
            containerColor = DragonTheme.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // 5. Delete Confirm Dialog
    modeToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { modeToDelete = null },
            title = {
                Text(
                    text = if (target.isCustom) "删除自定义场景" else "隐藏默认场景",
                    color = DragonTheme.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (target.isCustom) {
                        "确定要删除自定义场景「${target.title}」吗？场景内的收录记录将被清除。"
                    } else {
                        "确定要从列表隐藏「${target.title}」吗？您随时可点击右上角「恢复默认」重新找回。"
                    },
                    color = DragonTheme.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (target.isCustom) {
                            onDeleteCustomScenario(target.id)
                        } else {
                            onDeleteDefaultScenario(target.scenarioKey)
                        }
                        modeToDelete = null
                    }
                ) {
                    Text("确定", color = LiveRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { modeToDelete = null }) {
                    Text("取消", color = DragonTheme.textSecondary)
                }
            },
            containerColor = DragonTheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Clean modern vector icon selector with zero emojis
 */
@Composable
private fun ModernIconSelector(
    selectedKey: String,
    onSelectKey: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(AvailableModernIcons) { opt ->
            val isSelected = opt.key.equals(selectedKey, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) DragonCyan.copy(alpha = 0.18f) else DragonTheme.glassSurface,
                border = BorderStroke(1.dp, if (isSelected) DragonCyan else DragonTheme.glassBorder),
                modifier = Modifier.clickable { onSelectKey(opt.key) }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = opt.icon,
                        contentDescription = opt.label,
                        tint = if (isSelected) DragonCyan else DragonTheme.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = opt.label,
                        color = if (isSelected) DragonCyan else DragonTheme.textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

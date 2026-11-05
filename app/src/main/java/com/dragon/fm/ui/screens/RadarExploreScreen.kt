package com.dragon.fm.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.BookmarkAdd
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import androidx.compose.material.icons.outlined.BookmarkAdd
import com.dragon.fm.ui.components.AddToScenarioBottomSheet
import com.dragon.fm.data.model.ChineseTagHelper
import com.dragon.fm.data.model.CustomScenarioMode
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonGold
import com.dragon.fm.data.model.RealExploreTagGroup
import com.dragon.fm.data.model.RealExploreTagItem
import com.dragon.fm.data.model.RealTagHelper
import com.dragon.fm.ui.theme.DragonTheme
import com.dragon.fm.ui.viewmodel.MainViewModel

@Composable
fun RadarExploreScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showSaveModeDialog by remember { mutableStateOf(false) }
    var stationToAddToScenario by remember { mutableStateOf<RadioStation?>(null) }

    val selectedTag by viewModel.currentExploreTag.collectAsState()
    val selectedGroupIndex by viewModel.currentExploreGroupIndex.collectAsState()

    val exploreStations by viewModel.exploreStations.collectAsState()
    val radarSearchQuery by viewModel.radarSearchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val customScenarios by viewModel.customScenarios.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val scenarioOverrides by viewModel.scenarioStationOverrides.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || configuration.screenWidthDp >= 600

    val appBackground by viewModel.appBackground.collectAsState()
    val isDefaultBg = appBackground.type == com.dragon.fm.data.model.AppBackgroundType.DEFAULT

    // Data-driven exploration categories dynamically extracted from real live stations
    val tagGroups = remember { RealTagHelper.getTagGroups() }
    val currentGroup = tagGroups.getOrElse(selectedGroupIndex) { tagGroups[0] }

    // Only load initial stations if screen has never loaded any stations before
    LaunchedEffect(Unit) {
        if (radarSearchQuery.isBlank() && exploreStations.isEmpty()) {
            viewModel.loadExploreInitial()
        }
    }

    LaunchedEffect(radarSearchQuery) {
        if (radarSearchQuery.isNotBlank() && searchQuery != radarSearchQuery) {
            searchQuery = radarSearchQuery
        }
    }

    // 300ms Search Debounce to prevent competing network requests
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            if (radarSearchQuery.isNotBlank()) {
                viewModel.clearRadarQuery()
                viewModel.loadExploreInitial()
            }
        } else {
            delay(300)
            viewModel.setExploreTag("搜索: $q")
            viewModel.searchOnline(q)
        }
    }

    val onSelectSubTag: (RealExploreTagItem) -> Unit = { tagItem ->
        searchQuery = ""
        viewModel.searchByRealTag(tagItem)
    }

    if (isLandscape) {
        // ========================================================
        // LANDSCAPE DUAL-PANE LAYOUT: FULL HEIGHT SMOOTH SCROLLING
        // ========================================================
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isDefaultBg) Modifier.background(DragonTheme.background)
                    else Modifier.background(Color.Transparent)
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LEFT COLUMN: Search & Filter Console (35% Width)
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DragonTheme.surface.copy(alpha = if (DragonTheme.isDark) 0.5f else 0.85f))
                    .border(0.5.dp, DragonTheme.glassBorder, RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Header: Back button + title + Save as Mode button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                viewModel.clearRadarQuery()
                                onBack()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = DragonTheme.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "发现电台 • 探索",
                            color = DragonTheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        onClick = { showSaveModeDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = DragonCyan.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, DragonCyan.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "设为模式",
                                tint = DragonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "设为模式",
                                color = DragonCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Search Input Box
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = DragonTheme.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(DragonTheme.primary),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(DragonTheme.glassSurface)
                                .border(1.dp, DragonTheme.glassBorder, RoundedCornerShape(18.dp))
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = DragonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "搜索电台、省市或流派...",
                                        color = DragonTheme.textSecondary.copy(alpha = 0.6f),
                                        fontSize = 11.5.sp
                                    )
                                }
                                innerTextField()
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        viewModel.clearRadarQuery()
                                        viewModel.loadExploreInitial()
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = DragonTheme.textSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Group Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tagGroups.indices.toList()) { index ->
                        val group = tagGroups[index]
                        val isSelectedGroup = index == selectedGroupIndex
                        Surface(
                            onClick = { viewModel.setExploreGroupIndex(index) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelectedGroup) DragonTheme.primary.copy(alpha = 0.16f) else DragonTheme.glassSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelectedGroup) DragonTheme.primary else DragonTheme.glassBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = group.icon,
                                    contentDescription = null,
                                    tint = if (isSelectedGroup) DragonTheme.primary else DragonTheme.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = group.groupName,
                                    color = if (isSelectedGroup) DragonTheme.primary else DragonTheme.textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelectedGroup) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Sub-tags List (Scrollable column in left pane)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(currentGroup.tags) { tagItem ->
                        val isSelected = tagItem.title == selectedTag || tagItem.searchTerms.contains(selectedTag)
                        Surface(
                            onClick = { onSelectSubTag(tagItem) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) DragonCyan else DragonTheme.glassSurface,
                            border = BorderStroke(
                                if (isSelected) 1.dp else 0.5.dp,
                                if (isSelected) DragonCyan else DragonTheme.glassBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tagItem.title,
                                        color = if (isSelected) (if (DragonTheme.isDark) Color(0xFF070B12) else Color.White) else DragonTheme.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (DragonTheme.isDark) Color(0xFF070B12) else Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT COLUMN: Station Stream Grid with 100% Height for Silky Scrolling (65% Width)
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
            ) {
                // Results Info Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentGroup.groupName} • $selectedTag",
                        color = DragonTheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val countText = if (exploreStations.size >= 100) "发现 100+ 个电台" else "发现 ${exploreStations.size} 个电台"
                    Text(
                        text = if (isSearching) "正在极速检索..." else countText,
                        color = DragonTheme.textSecondary,
                        fontSize = 11.sp
                    )
                }

                // Station List Grid
                if (isSearching && exploreStations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = DragonCyan, modifier = Modifier.size(30.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "正在探索全球电台信号...", color = DragonTheme.textSecondary, fontSize = 12.sp)
                        }
                    }
                } else if (exploreStations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂未搜索到符合条件的电台，请尝试其他分类",
                            color = DragonTheme.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 280.dp),
                            contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(exploreStations, key = { it.id }) { station ->
                                StationExploreCard(
                                    station = station,
                                    onPlay = {
                                        viewModel.playStationFromExplore(station)
                                        onBack()
                                    },
                                    onAddToScenario = {
                                        stationToAddToScenario = station
                                    }
                                )
                            }
                        }
                        if (isSearching) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                                    .align(Alignment.TopCenter),
                                color = DragonCyan,
                                trackColor = Color.Transparent
                            )
                        }
                    }
                }
            }
        }
    } else {
        // ========================================================
        // PORTRAIT CLEAN STREAM LAYOUT
        // ========================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isDefaultBg) Modifier.background(DragonTheme.background)
                    else Modifier.background(Color.Transparent)
                )
                .statusBarsPadding()
        ) {
            // 1. Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            viewModel.clearRadarQuery()
                            onBack()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DragonTheme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "发现电台 • LX FM 雷达",
                        color = DragonTheme.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    onClick = { showSaveModeDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = DragonCyan.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, DragonCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "设为模式",
                            tint = DragonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "设为模式",
                            color = DragonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Modern Low-Profile Search Pill
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = DragonTheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(DragonTheme.primary),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(DragonTheme.glassSurface)
                            .border(1.dp, DragonTheme.glassBorder, RoundedCornerShape(19.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = DragonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "搜索电台名、省份城市、流派分类...",
                                    color = DragonTheme.textSecondary.copy(alpha = 0.6f),
                                    fontSize = 12.5.sp
                                )
                            }
                            innerTextField()
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    viewModel.clearRadarQuery()
                                    viewModel.loadExploreInitial()
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = DragonTheme.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // 3. Combined Tag Group Segment Tabs (Row 1: Category Groups)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            ) {
                items(tagGroups.indices.toList()) { index ->
                    val group = tagGroups[index]
                    val isSelectedGroup = index == selectedGroupIndex
                    Surface(
                        onClick = { viewModel.setExploreGroupIndex(index) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelectedGroup) DragonTheme.primary.copy(alpha = 0.16f) else DragonTheme.glassSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelectedGroup) DragonTheme.primary else DragonTheme.glassBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = group.icon,
                                contentDescription = null,
                                tint = if (isSelectedGroup) DragonTheme.primary else DragonTheme.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = group.groupName,
                                color = if (isSelectedGroup) DragonTheme.primary else DragonTheme.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelectedGroup) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 4. Combined Sub-Tags Capsule Row (Row 2: Fine-grained tags for current group)
            if (currentGroup.tags.size > 14) {
                // 双行紧凑胶囊流动布局（特别优化移动端 34 省市漫游横滑体验）
                val half = (currentGroup.tags.size + 1) / 2
                val row1 = currentGroup.tags.take(half)
                val row2 = currentGroup.tags.drop(half)
                Column(
                    modifier = Modifier.padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(row1) { tagItem ->
                            val isSelected = tagItem.title == selectedTag || tagItem.searchTerms.contains(selectedTag)
                            Surface(
                                onClick = { onSelectSubTag(tagItem) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) DragonTheme.primary.copy(alpha = 0.2f) else DragonTheme.glassSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(DragonTheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = tagItem.title,
                                        color = if (isSelected) DragonTheme.primary else DragonTheme.textSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(row2) { tagItem ->
                            val isSelected = tagItem.title == selectedTag || tagItem.searchTerms.contains(selectedTag)
                            Surface(
                                onClick = { onSelectSubTag(tagItem) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) DragonTheme.primary.copy(alpha = 0.2f) else DragonTheme.glassSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(DragonTheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = tagItem.title,
                                        color = if (isSelected) DragonTheme.primary else DragonTheme.textSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    items(currentGroup.tags) { tagItem ->
                        val isSelected = tagItem.title == selectedTag || tagItem.searchTerms.contains(selectedTag)
                        Surface(
                            onClick = { onSelectSubTag(tagItem) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) DragonTheme.primary.copy(alpha = 0.2f) else DragonTheme.glassSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(DragonTheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = tagItem.title,
                                    color = if (isSelected) DragonTheme.primary else DragonTheme.textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 5. Clean Results Header (No speed test or technical badges!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val countText = if (exploreStations.size >= 100) "发现 100+ 个电台频段" else "发现 ${exploreStations.size} 个电台频段"
                Text(
                    text = if (isSearching) "正在探索全球电台信号..." else countText,
                    color = DragonTheme.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 6. Stations List
            if (isSearching && exploreStations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DragonCyan, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "正在极速检索...", color = DragonTheme.textSecondary, fontSize = 12.5.sp)
                    }
                }
            } else if (exploreStations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂未搜索到符合条件的电台，请尝试其他关键词或省市",
                        color = DragonTheme.textSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        items(exploreStations, key = { it.id }) { station ->
                            StationExploreCard(
                                station = station,
                                onPlay = {
                                    viewModel.playStationFromExplore(station)
                                    onBack()
                                },
                                onAddToScenario = {
                                    stationToAddToScenario = station
                                }
                            )
                        }
                    }
                    if (isSearching) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .align(Alignment.TopCenter),
                            color = DragonCyan,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }
    }

    if (showSaveModeDialog) {
        var modeTitleInput by remember {
            mutableStateOf(if (searchQuery.isNotBlank()) searchQuery else selectedTag)
        }
        var selectedIconKey by remember { mutableStateOf("music") }
        var isReplacingExisting by remember { mutableStateOf(false) }
        var replaceModeId by remember { mutableStateOf(customScenarios.firstOrNull()?.id ?: "") }

        AlertDialog(
            onDismissRequest = { showSaveModeDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "设为收听模式",
                    color = DragonTheme.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "将当前探索「${if (searchQuery.isNotBlank()) searchQuery else selectedTag}」保存为常驻收听模式：",
                        color = DragonTheme.textSecondary,
                        fontSize = 12.5.sp
                    )

                    OutlinedTextField(
                        value = modeTitleInput,
                        onValueChange = { modeTitleInput = it },
                        label = { Text("模式名称") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "选择模式图标：",
                        color = DragonTheme.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(com.dragon.fm.data.model.AvailableModernIcons) { opt ->
                            val isChosen = selectedIconKey == opt.key
                            Surface(
                                onClick = { selectedIconKey = opt.key },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isChosen) DragonTheme.primary.copy(alpha = 0.2f) else DragonTheme.glassSurface,
                                border = BorderStroke(1.dp, if (isChosen) DragonTheme.primary else DragonTheme.glassBorder),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = opt.icon,
                                        contentDescription = opt.label,
                                        tint = if (isChosen) DragonTheme.primary else DragonTheme.textSecondary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (customScenarios.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                onClick = { isReplacingExisting = false },
                                shape = RoundedCornerShape(8.dp),
                                color = if (!isReplacingExisting) DragonTheme.primary.copy(alpha = 0.2f) else DragonTheme.glassSurface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "新建模式",
                                        color = if (!isReplacingExisting) DragonTheme.primary else DragonTheme.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                onClick = { isReplacingExisting = true },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isReplacingExisting) DragonTheme.primary.copy(alpha = 0.2f) else DragonTheme.glassSurface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "替换已有",
                                        color = if (isReplacingExisting) DragonTheme.primary else DragonTheme.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (isReplacingExisting) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(customScenarios) { existing ->
                                    val isSelected = replaceModeId == existing.id
                                    Surface(
                                        onClick = {
                                            replaceModeId = existing.id
                                            modeTitleInput = existing.title
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) DragonTheme.primary.copy(alpha = 0.2f) else DragonTheme.glassSurface,
                                        border = BorderStroke(1.dp, if (isSelected) DragonTheme.primary else DragonTheme.glassBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = existing.iconVector,
                                                contentDescription = null,
                                                tint = if (isSelected) DragonTheme.primary else DragonTheme.textSecondary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = existing.title,
                                                color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val query = if (searchQuery.isNotBlank()) searchQuery.trim() else selectedTag
                        val finalTitle = modeTitleInput.trim().ifBlank { query }
                        val replaceTarget = if (isReplacingExisting) replaceModeId else null
                        viewModel.saveCustomScenario(
                            title = finalTitle,
                            iconEmoji = selectedIconKey,
                            query = query,
                            replaceId = replaceTarget
                        )
                        showSaveModeDialog = false
                        Toast.makeText(context, "已成功保存「$finalTitle」模式！", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DragonTheme.primary)
                ) {
                    Text("保存生效", color = if (DragonTheme.isDark) Color(0xFF070B12) else Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveModeDialog = false }) {
                    Text("取消", color = DragonTheme.textSecondary)
                }
            },
            containerColor = DragonTheme.surface
        )
    }

    if (stationToAddToScenario != null) {
        val targetStation = stationToAddToScenario!!
        AddToScenarioBottomSheet(
            station = targetStation,
            customScenarios = customScenarios,
            scenarioOverrides = scenarioOverrides,
            favoriteIds = favoriteIds,
            onToggleInScenario = { scenarioName ->
                viewModel.toggleStationInScenario(scenarioName, targetStation)
            },
            onCreateScenarioWithStation = { title, iconKey ->
                viewModel.saveCustomScenario(
                    title = title,
                    iconEmoji = iconKey,
                    initialStation = targetStation
                )
            },
            onDismiss = { stationToAddToScenario = null }
        )
    }
}

@Composable
fun StationExploreCard(
    station: RadioStation,
    onPlay: () -> Unit,
    onAddToScenario: () -> Unit
) {
    Surface(
        color = DragonTheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DragonTheme.glassBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station Favicon
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
                        imageVector = Icons.Outlined.Radio,
                        contentDescription = "Radio",
                        tint = DragonTheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Station Metadata
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = station.name,
                        color = DragonTheme.textPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (station.hasDedicatedLine) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = DragonCyan.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.6.dp, DragonCyan.copy(alpha = 0.45f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(DragonCyan)
                                )
                                Spacer(modifier = Modifier.width(3.5.dp))
                                Text(
                                    text = "高品质专线",
                                    color = DragonCyan,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    } else if (station.codec.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = DragonTheme.glassSurface,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, DragonTheme.glassBorder)
                        ) {
                            Text(
                                text = station.codec,
                                color = DragonTheme.textSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }

                // Real-time EPG program title if available
                if (!station.currentProgram.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "正在直播: ${station.currentProgram}",
                        color = DragonTheme.primary.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Chinese Tag Mode in card
                val cardTags = remember(station.id) {
                    ChineseTagHelper.extractChineseTags(station).take(3)
                }
                if (cardTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        cardTags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (tag.isRegion) DragonTheme.primary.copy(alpha = 0.12f) else DragonTheme.glassSurface
                            ) {
                                Text(
                                    text = tag.name,
                                    color = if (tag.isRegion) DragonTheme.primary else DragonTheme.textSecondary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Bookmark Add to Scenario Button
                IconButton(
                    onClick = onAddToScenario,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkAdd,
                        contentDescription = "收藏到场景",
                        tint = DragonTheme.textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Play Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DragonCyan.copy(alpha = 0.15f))
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = DragonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

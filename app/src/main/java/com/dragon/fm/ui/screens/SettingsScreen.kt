package com.dragon.fm.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import com.dragon.fm.data.model.AppBackgroundType
import com.dragon.fm.data.model.BackgroundPresets
import com.dragon.fm.data.model.BackgroundPreset
import com.dragon.fm.data.model.StreamSourceMode
import com.dragon.fm.data.model.UpdateCheckResult
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.data.model.ChinaRegionData
import com.dragon.fm.data.model.ProvinceRegion
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonGold
import com.dragon.fm.ui.theme.DragonTheme
import com.dragon.fm.ui.theme.LiveRed
import com.dragon.fm.ui.theme.ThemePreset
import com.dragon.fm.ui.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val isDomesticOnly by viewModel.isDomesticOnly.collectAsState()
    val isFastBuffer by viewModel.isFastBuffer.collectAsState()
    val isPlaybackMemoryEnabled by viewModel.isPlaybackMemoryEnabled.collectAsState()
    val blockedStations by viewModel.blockedStations.collectAsState()
    val blockedTags by viewModel.blockedTags.collectAsState()
    val userProvince by viewModel.userSelectedProvince.collectAsState()
    val userCity by viewModel.userSelectedCity.collectAsState()
    val isCustomRegion by viewModel.isCustomRegionEnabled.collectAsState()
    val detectedProvince by viewModel.detectedProvince.collectAsState()
    val detectedCity by viewModel.detectedCity.collectAsState()
    val imageCacheBytes by viewModel.imageCacheSizeBytes.collectAsState()
    val historyStations by viewModel.historyStations.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val isFollowSystemTheme by viewModel.isFollowSystemTheme.collectAsState()
    val appBackground by viewModel.appBackground.collectAsState()
    val isAutoPlayOnLaunch by viewModel.isAutoPlayOnLaunch.collectAsState()
    val autoUpdateEnabled by viewModel.autoUpdateEnabled.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val streamSourceMode by viewModel.streamSourceMode.collectAsState()

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setCustomBackgroundImage(it.toString())
            Toast.makeText(context, "自定义壁纸已应用", Toast.LENGTH_SHORT).show()
        }
    }

    var showRegionPicker by remember { mutableStateOf(false) }
    var showStreamSourceDialog by remember { mutableStateOf(false) }
    var showBlockedStationsDialog by remember { mutableStateOf(false) }
    var showBlockedTagsDialog by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var showQuitConfirm by remember { mutableStateOf(false) }

    val updateCheckResult by viewModel.updateCheckResult.collectAsState()

    LaunchedEffect(updateCheckResult) {
        when (val res = updateCheckResult) {
            is UpdateCheckResult.UpToDate -> {
                if (res.isManual) {
                    Toast.makeText(context, "当前已是最新版本 (v${res.currentVersion})", Toast.LENGTH_SHORT).show()
                }
            }
            is UpdateCheckResult.Error -> {
                if (res.isManual) {
                    Toast.makeText(context, "检测更新失败: ${res.message}", Toast.LENGTH_SHORT).show()
                }
            }
            is UpdateCheckResult.HasUpdate -> {
                if (res.isManual) {
                    Toast.makeText(context, "发现新版本: v${res.info.versionName}", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {}
        }
    }

    val openGithubRepo = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/zzg341125/QY-FM"))
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "请在浏览器访问: https://github.com/zzg341125/QY-FM", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize()
    }

    val regionDisplayText = if (isCustomRegion && !userProvince.isNullOrBlank()) {
        val cityPart = if (userCity.isNullOrBlank() || userCity == "全城通用" || userCity == "全省通用" || userCity == "全区通用") "" else " · $userCity"
        "$userProvince$cityPart (自定义)"
    } else {
        val detectedText = if (!detectedProvince.isNullOrBlank()) {
            val cityStr = if (!detectedCity.isNullOrBlank() && detectedCity != detectedProvince) "-$detectedCity" else ""
            "$detectedProvince$cityStr"
        } else {
            "跟随网络"
        }
        "自动定位: $detectedText"
    }

    val cacheDisplay = if (imageCacheBytes > 0) {
        String.format("%.1f MB", imageCacheBytes / (1024f * 1024f))
    } else {
        "0.0 MB"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (appBackground.type == AppBackgroundType.DEFAULT) Modifier.background(DragonTheme.background)
                else Modifier.background(Color.Transparent)
            )
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DragonTheme.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "系统与偏好设置",
                    color = DragonTheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "广播定位、播放体验与个性化管理",
                    color = DragonTheme.textSecondary,
                    fontSize = 11.5.sp
                )
            }
        }

        // Settings Body
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Section 1: Region & Localization
            item {
                SettingsSectionHeader(title = "地区与广播定位", icon = Icons.Default.LocationOn)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    // Region Picker Entry
                    SettingsActionRow(
                        title = "我的偏好地区 (支持选择二级市)",
                        subtitle = regionDisplayText,
                        actionLabel = "更换地区",
                        onClick = { showRegionPicker = true }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    // Domestic Only Switch
                    SettingsSwitchRow(
                        title = "仅加载国内电台",
                        subtitle = "自动过滤海外频段，专注收听央广、省市及各地方高品质广播",
                        checked = isDomesticOnly,
                        onCheckedChange = { viewModel.setDomesticOnly(it) }
                    )
                }
            }

            // Section 2: Audio & Playback Engine
            item {
                SettingsSectionHeader(title = "播放与音频引擎", icon = Icons.Default.Speed)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsActionRow(
                        title = "直播源与线路偏好",
                        subtitle = when (streamSourceMode) {
                            StreamSourceMode.HYBRID -> "智能极速模式 · 官方专线优先，公网自动容灾"
                            StreamSourceMode.DEDICATED_ONLY -> "纯净官方专线 · 仅使用官方高可靠极速专线"
                            StreamSourceMode.PUBLIC_ONLY -> "全球公网广播 · 仅使用开放公共广播源"
                        },
                        actionLabel = when (streamSourceMode) {
                            StreamSourceMode.HYBRID -> "智能极速"
                            StreamSourceMode.DEDICATED_ONLY -> "纯净专线"
                            StreamSourceMode.PUBLIC_ONLY -> "全球公网"
                        },
                        actionColor = DragonCyan,
                        onClick = { showStreamSourceDialog = true }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsSwitchRow(
                        title = "0.25s 极速起播优化",
                        subtitle = "超低缓冲延迟配置，切台如转动物理收音机旋钮般灵敏无顿挫",
                        checked = isFastBuffer,
                        onCheckedChange = { viewModel.setFastBuffer(it) }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsSwitchRow(
                        title = "启动时自动播放电台",
                        subtitle = "开启后打开应用立即播放电台；关闭后应用准备就绪但不立即发出声音",
                        checked = isAutoPlayOnLaunch,
                        onCheckedChange = { viewModel.setAutoPlayOnLaunch(it) }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsSwitchRow(
                        title = "记忆收听进度与播放列表",
                        subtitle = "下次打开应用时自动恢复最后收听的电台频道、模式场景与播放队列",
                        checked = isPlaybackMemoryEnabled,
                        onCheckedChange = { viewModel.setPlaybackMemoryEnabled(it) }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsInfoRow(
                        title = "音频设备拔出自动暂停",
                        subtitle = "拔出耳机或断开车载蓝牙时静默暂停，避免公共场合声音外放",
                        statusText = "已开启"
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsInfoRow(
                        title = "车载 MediaSession 控制",
                        subtitle = "全面集成 Android 原生媒体中心，支持方向盘按键与锁屏切台",
                        statusText = "已就绪"
                    )
                }
            }

            // Section 3: Content Filtering & Blacklist
            item {
                SettingsSectionHeader(title = "内容过滤与电台屏蔽", icon = Icons.Default.Block)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsActionRow(
                        title = "已屏蔽电台黑名单",
                        subtitle = "管理已拉黑的电台 (${blockedStations.size} 个)，漫游时将自动跳过",
                        actionLabel = "查看管理",
                        onClick = { showBlockedStationsDialog = true }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        title = "屏蔽分类与关键词标签",
                        subtitle = "包含所选标签的电台将自动跳过 (${blockedTags.size} 个标签)",
                        actionLabel = "查看管理",
                        onClick = { showBlockedTagsDialog = true }
                    )
                }
            }

            // Section 4: Themes & Modes
            item {
                SettingsSectionHeader(title = "界面与个性化", icon = Icons.Default.Tune)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    // 1. Follow System Dark/Light Theme Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setFollowSystemTheme(!isFollowSystemTheme) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "跟随系统深浅外观",
                                color = DragonTheme.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isFollowSystemTheme) "已开启：随系统浅色/深色模式自动无缝切换" else "已关闭：固定使用所选主题风格",
                                color = DragonTheme.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isFollowSystemTheme,
                            onCheckedChange = { viewModel.setFollowSystemTheme(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DragonTheme.primary,
                                checkedTrackColor = DragonTheme.primary.copy(alpha = 0.35f),
                                uncheckedThumbColor = DragonTheme.textSecondary,
                                uncheckedTrackColor = DragonTheme.glassSurface
                            )
                        )
                    }

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    // 2. Light Theme Presets (专为播放器打造的高颜值浅色主题)
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BrightnessAuto,
                                contentDescription = null,
                                tint = DragonGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "高颜值浅色主题",
                                color = DragonTheme.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "清爽通透 · 护眼声学",
                                color = DragonTheme.textSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (theme in ThemePreset.lightPresets) {
                                val isSelected = theme.id == currentTheme.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) DragonTheme.primary.copy(alpha = 0.15f) else DragonTheme.glassSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.setTheme(theme) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(theme.indicatorColor)
                                                .border(1.dp, DragonTheme.glassBorder, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = theme.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    // 3. Dark Theme Presets (沉浸深色主题)
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = DragonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "沉浸深色主题",
                                color = DragonTheme.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "纯粹深空 · 极客黑曜",
                                color = DragonTheme.textSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (theme in ThemePreset.darkPresets) {
                                val isSelected = theme.id == currentTheme.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) DragonTheme.primary.copy(alpha = 0.15f) else DragonTheme.glassSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.setTheme(theme) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(theme.indicatorColor)
                                                .border(1.dp, DragonTheme.glassBorder, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = theme.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    // 4. Background & Custom Wallpaper Settings
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = null,
                                tint = DragonTheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "背景与个性化壁纸",
                                color = DragonTheme.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (appBackground.type) {
                                AppBackgroundType.DEFAULT -> "当前：默认主题纯色底色"
                                AppBackgroundType.PRESET -> "当前预设：${BackgroundPresets.getById(appBackground.presetId).title}"
                                AppBackgroundType.CUSTOM -> "当前：自定义相册壁纸"
                            },
                            color = DragonTheme.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Wallpaper Actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Pick from Album
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (appBackground.type == AppBackgroundType.CUSTOM) DragonTheme.primary.copy(alpha = 0.18f) else DragonTheme.glassSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (appBackground.type == AppBackgroundType.CUSTOM) DragonTheme.primary else DragonTheme.glassBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { wallpaperPickerLauncher.launch("image/*") }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = if (appBackground.type == AppBackgroundType.CUSTOM) DragonTheme.primary else DragonTheme.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "相册选图壁纸",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (appBackground.type == AppBackgroundType.CUSTOM) DragonTheme.primary else DragonTheme.textPrimary
                                    )
                                }
                            }

                            // Restore Solid Default
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (appBackground.type == AppBackgroundType.DEFAULT) DragonTheme.primary.copy(alpha = 0.18f) else DragonTheme.glassSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (appBackground.type == AppBackgroundType.DEFAULT) DragonTheme.primary else DragonTheme.glassBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.resetBackgroundToDefault()
                                        Toast.makeText(context, "已恢复纯色默认背景", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(DragonTheme.background)
                                            .border(1.dp, DragonTheme.glassBorder, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "恢复纯色背景",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (appBackground.type == AppBackgroundType.DEFAULT) DragonTheme.primary else DragonTheme.textPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Preset fluid backgrounds
                        Text(
                            text = "内置精选流光背景预设：",
                            color = DragonTheme.textSecondary,
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (preset in BackgroundPresets.PRESETS) {
                                val isSelected = appBackground.type == AppBackgroundType.PRESET && appBackground.presetId == preset.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) DragonTheme.primary.copy(alpha = 0.2f) else DragonTheme.glassSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.setBackgroundPreset(preset.id)
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(preset.brush)
                                                .border(1.dp, DragonTheme.glassBorder, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = preset.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    // Font Size Scale Adjustment
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "界面字体大小调节",
                                color = DragonTheme.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            val currentScaleName = when {
                                fontScale <= 0.90f -> "紧凑 (88%)"
                                fontScale <= 1.05f -> "标准 (100%)"
                                fontScale <= 1.18f -> "舒适 (112%)"
                                else -> "特大 (125%)"
                            }
                            Text(
                                text = currentScaleName,
                                color = DragonTheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "完美自适应不同屏幕尺寸与横竖屏模式，防止文字溢出或卡片截断",
                            color = DragonTheme.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val fontOptions = listOf(
                            Triple("紧凑", 0.88f, "88%"),
                            Triple("标准", 1.00f, "100%"),
                            Triple("舒适", 1.12f, "112%"),
                            Triple("特大", 1.25f, "125%")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            fontOptions.forEach { (label, scaleVal, pct) ->
                                val isSelected = kotlin.math.abs(fontScale - scaleVal) < 0.05f
                                Surface(
                                    onClick = { viewModel.setFontScale(scaleVal) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) DragonTheme.primary.copy(alpha = 0.16f) else DragonTheme.glassSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary
                                        )
                                        Text(
                                            text = pct,
                                            fontSize = 10.sp,
                                            color = if (isSelected) DragonTheme.primary else DragonTheme.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        title = "添加自定义网络广播流",
                        subtitle = "手动添加支持 M3U8、MP3、AAC 格式的私有或小众电台",
                        actionLabel = "添加流",
                        onClick = { showAddCustomDialog = true }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        title = "恢复全部默认收听场景",
                        subtitle = "若误删除了「潮流通勤」「晚间助眠」等预设模式，可一键重置",
                        actionLabel = "一键恢复",
                        onClick = {
                            viewModel.restoreDefaultScenarios()
                            Toast.makeText(context, "所有预设收听模式已重置恢复", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Section 5: Storage & Cache Management
            item {
                SettingsSectionHeader(title = "存储与缓存管理", icon = Icons.Default.Storage)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsActionRow(
                        title = "电台台标与封面缓存",
                        subtitle = "当前磁盘占用：$cacheDisplay (采用 LRU 自动清理)",
                        actionLabel = "清理缓存",
                        onClick = {
                            val freed = viewModel.clearImageCache()
                            val freedMb = String.format("%.1f MB", freed / (1024f * 1024f))
                            Toast.makeText(context, "已清理 $freedMb 图片缓存", Toast.LENGTH_SHORT).show()
                        }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        title = "收听历史足迹",
                        subtitle = "已记录 ${historyStations.size} 个电台（上限 60 条，约 15 KB）",
                        actionLabel = "清空足迹",
                        onClick = { showClearHistoryConfirm = true }
                    )
                }
            }

            // Section 6: Version & Open Source Updates
            item {
                SettingsSectionHeader(title = "版本与开源更新", icon = Icons.Default.SystemUpdate)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsActionRow(
                        title = "检查新版本",
                        subtitle = "当前版本: v${viewModel.currentVersionName} (Build ${viewModel.currentVersionCode}) · GitHub Releases",
                        actionLabel = if (updateCheckResult is UpdateCheckResult.Checking) "检查中..." else "检查更新",
                        actionColor = DragonTheme.primary,
                        onClick = {
                            viewModel.checkForUpdates(isManual = true)
                        }
                    )
                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)
                    SettingsSwitchRow(
                        title = "自动检查并下载更新",
                        subtitle = "启动后自动检查并下载 GitHub 新版本；安装由 Android 系统确认",
                        checked = autoUpdateEnabled,
                        onCheckedChange = { viewModel.setAutoUpdateEnabled(it) }
                    )
                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)
                    SettingsActionRow(
                        title = "GitHub 开源项目",
                        subtitle = "https://github.com/zzg341125/QY-FM",
                        actionLabel = "访问",
                        actionColor = DragonCyan,
                        onClick = openGithubRepo
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Section 7: Application Control
            item {
                SettingsSectionHeader(title = "应用控制", icon = Icons.Default.PowerSettingsNew)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsActionRow(
                        title = "完全退出应用程序",
                        subtitle = "停止所有音频流、销毁通知栏后台服务并释放运行内存",
                        actionLabel = "退出",
                        actionColor = LiveRed,
                        onClick = { showQuitConfirm = true }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Secondary City Picker Dialog (省市二级联动选择器)
    if (showRegionPicker) {
        CityPickerDialog(
            currentProvince = userProvince,
            currentCity = userCity,
            defaultProvince = detectedProvince,
            defaultCity = detectedCity,
            isCustom = isCustomRegion,
            onSelect = { prov, city ->
                viewModel.setUserRegion(prov, city)
                showRegionPicker = false
                Toast.makeText(context, "已将地区设定为「$prov · $city」", Toast.LENGTH_SHORT).show()
            },
            onResetAuto = {
                viewModel.resetToAutoLocation()
                showRegionPicker = false
                Toast.makeText(context, "已恢复网络自动定位", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showRegionPicker = false }
        )
    }

    // Stream Source Mode Dialog (直播源与线路偏好)
    if (showStreamSourceDialog) {
        StreamSourceModeDialog(
            currentMode = streamSourceMode,
            onSelect = { mode ->
                viewModel.setStreamSourceMode(mode)
                showStreamSourceDialog = false
                val toastDesc = when (mode) {
                    StreamSourceMode.HYBRID -> "已启用智能极速模式（官方专线优先，自动容灾）"
                    StreamSourceMode.DEDICATED_ONLY -> "已启用纯净官方专线模式"
                    StreamSourceMode.PUBLIC_ONLY -> "已切换为全球公网广播源"
                }
                Toast.makeText(context, toastDesc, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showStreamSourceDialog = false }
        )
    }

    // Blocked Stations Dialog
    if (showBlockedStationsDialog) {
        BlockedStationsManagerDialog(
            blockedStations = blockedStations,
            onUnblock = { viewModel.unblockStation(it) },
            onBlockByName = { viewModel.blockStationByName(it) },
            onClearAll = { viewModel.clearAllBlockedStations() },
            onDismiss = { showBlockedStationsDialog = false }
        )
    }

    // Blocked Tags Dialog
    if (showBlockedTagsDialog) {
        BlockedTagsManagerDialog(
            blockedTags = blockedTags,
            onUnblock = { viewModel.unblockTag(it) },
            onBlockTag = { viewModel.blockTag(it) },
            onDismiss = { showBlockedTagsDialog = false }
        )
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
                        label = { Text("直播流 URL (m3u8/mp3/aac)") },
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
                            Toast.makeText(context, "已添加并起播「$nameInput」", Toast.LENGTH_SHORT).show()
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

    // Clear History Dialog
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

    // Quit Application Dialog
    if (showQuitConfirm) {
        AlertDialog(
            onDismissRequest = { showQuitConfirm = false },
            containerColor = DragonTheme.surface,
            title = { Text("退出 LX FM", color = DragonTheme.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "确定要停止电台播放并完全退出应用程序吗？",
                    color = DragonTheme.textSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitConfirm = false
                        viewModel.quitApplication(context as? Activity)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                ) {
                    Text("退出应用", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitConfirm = false }) {
                    Text("取消", color = DragonTheme.textSecondary)
                }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Sub-components: Cards, Dialogs & Rows
// -------------------------------------------------------------------------------------------------

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DragonTheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = DragonTheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DragonTheme.surface,
        border = BorderStroke(1.dp, DragonTheme.glassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    actionColor: Color = DragonCyan,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                color = DragonTheme.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = DragonTheme.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.5.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = actionLabel,
                color = actionColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = actionColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = DragonTheme.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = DragonTheme.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.5.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF070B12),
                checkedTrackColor = DragonCyan
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(
    title: String,
    subtitle: String,
    statusText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                color = DragonTheme.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = DragonTheme.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.5.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DragonTheme.glassSurface,
            border = BorderStroke(0.5.dp, DragonTheme.glassBorder)
        ) {
            Text(
                text = statusText,
                color = DragonTheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Two-tier Province and Secondary City Picker Dialog
 */
@Composable
private fun CityPickerDialog(
    currentProvince: String?,
    currentCity: String?,
    defaultProvince: String? = null,
    defaultCity: String? = null,
    isCustom: Boolean,
    onSelect: (province: String, city: String) -> Unit,
    onResetAuto: () -> Unit,
    onDismiss: () -> Unit
) {
    val initialProvName = (if (isCustom && !currentProvince.isNullOrBlank()) currentProvince else defaultProvince) ?: "河南省"
    val cleanInitial = initialProvName.replace("省", "").replace("市", "").replace("自治区", "").replace("特别行政区", "").trim()
    val matchedProv = ChinaRegionData.allRegions.find {
        it.province.contains(cleanInitial)
    }?.province ?: ChinaRegionData.allRegions.first().province

    var selectedProvince by remember { mutableStateOf(matchedProv) }
    val cities = remember(selectedProvince) {
        ChinaRegionData.getCities(selectedProvince)
    }

    val targetCityClean = (if (isCustom && !currentCity.isNullOrBlank()) currentCity else defaultCity)
        ?.replace("市", "")?.replace("区", "")?.replace("县", "")?.replace("地区", "")?.trim()

    val provinceListState = rememberLazyListState()
    LaunchedEffect(matchedProv) {
        val idx = ChinaRegionData.allRegions.indexOfFirst { it.province == matchedProv }
        if (idx >= 0) {
            provinceListState.scrollToItem((idx - 2).coerceAtLeast(0))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DragonTheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择省份与二级市",
                    color = DragonTheme.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onResetAuto) {
                    Text("自动定位", color = DragonCyan, fontSize = 12.sp)
                }
            }
        },
        text = {
            Column(modifier = Modifier.height(360.dp)) {
                Text(
                    text = "当前选择：$selectedProvince",
                    color = DragonTheme.primary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Left Column: Provinces
                    LazyColumn(
                        state = provinceListState,
                        modifier = Modifier
                            .weight(0.48f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DragonTheme.glassSurface)
                            .padding(4.dp)
                    ) {
                        items(ChinaRegionData.allRegions) { reg ->
                            val isSel = reg.province == selectedProvince
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) DragonTheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedProvince = reg.province }
                            ) {
                                Text(
                                    text = reg.province,
                                    color = if (isSel) DragonTheme.primary else DragonTheme.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right Column: Secondary Cities
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.52f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DragonTheme.glassSurface)
                            .padding(4.dp)
                    ) {
                        items(cities) { city ->
                            val isCitySel = (isCustom && selectedProvince == currentProvince && city == currentCity) ||
                                    (!isCustom && selectedProvince == matchedProv && !targetCityClean.isNullOrBlank() && city.contains(targetCityClean))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isCitySel) DragonCyan.copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(selectedProvince, city)
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = city,
                                        color = if (isCitySel) DragonCyan else DragonTheme.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCitySel) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isCitySel) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = DragonCyan,
                                            modifier = Modifier.size(14.dp)
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
            TextButton(onClick = onDismiss) {
                Text("关闭", color = DragonTheme.textSecondary)
            }
        }
    )
}

/**
 * Blocked Stations Management Dialog
 */
@Composable
private fun BlockedStationsManagerDialog(
    blockedStations: Map<String, String>,
    onUnblock: (String) -> Unit,
    onBlockByName: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var inputName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DragonTheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已屏蔽电台 (${blockedStations.size})",
                    color = DragonTheme.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (blockedStations.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("清空全部", color = LiveRed, fontSize = 12.sp)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.height(320.dp)) {
                // Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        placeholder = { Text("输入电台名称添加屏蔽", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (inputName.isNotBlank()) {
                                onBlockByName(inputName)
                                inputName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DragonCyan, contentColor = Color(0xFF070B12))
                    ) {
                        Text("屏蔽", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (blockedStations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无屏蔽电台", color = DragonTheme.textSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(blockedStations.toList()) { (id, name) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DragonTheme.glassSurface,
                                border = BorderStroke(0.5.dp, DragonTheme.glassBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        color = DragonTheme.textPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { onUnblock(id) }) {
                                        Text("解除", color = DragonCyan, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = DragonTheme.textPrimary)
            }
        }
    )
}

/**
 * Blocked Tags Management Dialog
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockedTagsManagerDialog(
    blockedTags: Set<String>,
    onUnblock: (String) -> Unit,
    onBlockTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputTag by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DragonTheme.surface,
        title = {
            Text(
                text = "屏蔽标签与流派 (${blockedTags.size})",
                color = DragonTheme.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.height(300.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputTag,
                        onValueChange = { inputTag = it },
                        placeholder = { Text("输入关键词或标签", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (inputTag.isNotBlank()) {
                                onBlockTag(inputTag)
                                inputTag = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DragonCyan, contentColor = Color(0xFF070B12))
                    ) {
                        Text("添加", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (blockedTags.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("未设置任何屏蔽标签", color = DragonTheme.textSecondary, fontSize = 13.sp)
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        blockedTags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DragonTheme.glassSurface,
                                border = BorderStroke(0.5.dp, DragonTheme.glassBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = tag, color = DragonTheme.textPrimary, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "删除",
                                        tint = LiveRed,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onUnblock(tag) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = DragonTheme.textPrimary)
            }
        }
    )
}

/**
 * Stream Source & Line Preference Selection Dialog
 */
@Composable
private fun StreamSourceModeDialog(
    currentMode: StreamSourceMode,
    onSelect: (StreamSourceMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DragonTheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = DragonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "选择直播源与线路偏好",
                    color = DragonTheme.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StreamSourceMode.values().forEach { mode ->
                    val isSelected = (mode == currentMode)
                    Surface(
                        onClick = { onSelect(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) DragonCyan.copy(alpha = 0.12f) else DragonTheme.glassSurface,
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 0.5.dp,
                            if (isSelected) DragonCyan else DragonTheme.glassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mode.title,
                                        color = if (isSelected) DragonCyan else DragonTheme.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (mode == StreamSourceMode.HYBRID) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = DragonCyan.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "推荐",
                                                color = DragonCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = mode.desc,
                                    color = DragonTheme.textSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelect(mode) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = DragonCyan,
                                    unselectedColor = DragonTheme.textSecondary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = DragonCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}

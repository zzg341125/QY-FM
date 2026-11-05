package com.dragon.fm.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dragon.fm.data.model.AvailableModernIcons
import com.dragon.fm.data.model.CustomScenarioMode
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonTheme

data class ScenarioTargetItem(
    val id: String,
    val title: String,
    val iconVector: ImageVector,
    val stationCount: Int,
    val isCustom: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToScenarioBottomSheet(
    station: RadioStation,
    customScenarios: List<CustomScenarioMode>,
    scenarioOverrides: Map<String, List<RadioStation>>,
    favoriteIds: Set<String>,
    onToggleInScenario: (String) -> Unit,
    onCreateScenarioWithStation: (title: String, iconKey: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.82f).dp

    var isCreatingNew by remember { mutableStateOf(false) }
    var newScenarioTitle by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf("music") }

    // Built-in scenario entries
    val builtinScenarios = listOf(
        ScenarioTargetItem("builtin_fav", "我的收藏", Icons.Outlined.Star, favoriteIds.size),
        ScenarioTargetItem("builtin_featured", "精选推荐", Icons.Outlined.AutoAwesome, scenarioOverrides["精选推荐"]?.size ?: 18),
        ScenarioTargetItem("builtin_commute", "潮流通勤", Icons.Outlined.MusicNote, scenarioOverrides["潮流通勤"]?.size ?: 12),
        ScenarioTargetItem("builtin_traffic", "路况交通", Icons.Outlined.DirectionsCar, scenarioOverrides["路况交通"]?.size ?: 10),
        ScenarioTargetItem("builtin_sleep", "晚间助眠", Icons.Outlined.Bedtime, scenarioOverrides["晚间助眠"]?.size ?: 8),
        ScenarioTargetItem("builtin_focus", "咖啡专注", Icons.Outlined.LocalCafe, scenarioOverrides["咖啡专注"]?.size ?: 8),
        ScenarioTargetItem("builtin_news", "实时新闻", Icons.Outlined.Mic, scenarioOverrides["实时新闻"]?.size ?: 10),
        ScenarioTargetItem("builtin_classic", "华语经典", Icons.Outlined.MusicNote, scenarioOverrides["华语经典"]?.size ?: 12),
        ScenarioTargetItem("builtin_rock", "摇滚动感", Icons.Outlined.Speed, scenarioOverrides["摇滚动感"]?.size ?: 10),
        ScenarioTargetItem("builtin_world", "环球视界", Icons.Outlined.Public, scenarioOverrides["环球视界"]?.size ?: 10),
        ScenarioTargetItem("builtin_local", "本地广播", Icons.Outlined.NearMe, scenarioOverrides["本地电台"]?.size ?: 8)
    )

    val customItems = customScenarios.map { c ->
        ScenarioTargetItem(
            id = c.id,
            title = c.title,
            iconVector = c.iconVector,
            stationCount = c.stations.size,
            isCustom = true
        )
    }

    val allScenarioTargets = customItems + builtinScenarios

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header: Station Info Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
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
                                contentDescription = null,
                                tint = DragonTheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = station.name,
                            color = DragonTheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "选择要收录此电台的收听场景",
                            color = DragonTheme.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = DragonTheme.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Create New Scenario Inline Button
            Surface(
                onClick = { isCreatingNew = !isCreatingNew },
                shape = RoundedCornerShape(12.dp),
                color = DragonTheme.primary.copy(alpha = if (isCreatingNew) 0.16f else 0.08f),
                border = BorderStroke(1.dp, DragonTheme.primary.copy(alpha = if (isCreatingNew) 0.8f else 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(DragonTheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCreatingNew) Icons.Outlined.Close else Icons.Outlined.Add,
                                contentDescription = null,
                                tint = DragonTheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isCreatingNew) "收起新建面板" else "+ 新建场景并收录此频道",
                            color = DragonTheme.primary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Inline Create Scenario Form
            AnimatedVisibility(
                visible = isCreatingNew,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DragonTheme.glassSurface)
                        .border(1.dp, DragonTheme.glassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "新建个性化收听场景",
                        color = DragonTheme.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = newScenarioTitle,
                        onValueChange = { newScenarioTitle = it },
                        placeholder = { Text("输入场景名称，如：大湾区经典、深夜白噪音...", fontSize = 12.5.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DragonTheme.primary,
                            unfocusedBorderColor = DragonTheme.glassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "选择场景专属线条图标：",
                        color = DragonTheme.textSecondary,
                        fontSize = 11.5.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AvailableModernIcons) { opt ->
                            val isSelected = selectedIconKey == opt.key
                            Surface(
                                onClick = { selectedIconKey = opt.key },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) DragonTheme.primary.copy(alpha = 0.22f) else DragonTheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) DragonTheme.primary else DragonTheme.glassBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = opt.icon,
                                        contentDescription = opt.label,
                                        tint = if (isSelected) DragonTheme.primary else DragonTheme.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = opt.label,
                                        color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val title = newScenarioTitle.trim()
                            if (title.isBlank()) {
                                Toast.makeText(context, "请输入场景名称", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onCreateScenarioWithStation(title, selectedIconKey)
                            Toast.makeText(context, "已创建「$title」并收录！", Toast.LENGTH_SHORT).show()
                            isCreatingNew = false
                            newScenarioTitle = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DragonTheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "立即创建并收录",
                            color = if (DragonTheme.isDark) Color(0xFF070B12) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "所有收听场景",
                color = DragonTheme.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
            )

            // Scenarios Target List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allScenarioTargets, key = { it.id }) { target ->
                    val isIncluded = if (target.title == "我的收藏") {
                        favoriteIds.contains(station.id)
                    } else if (target.isCustom) {
                        customScenarios.find { it.id == target.id }?.stations?.any { it.id == station.id } == true
                    } else {
                        scenarioOverrides[target.title]?.any { it.id == station.id } == true
                    }

                    Surface(
                        onClick = {
                            onToggleInScenario(target.title)
                            val feedback = if (!isIncluded) "已加入「${target.title}」" else "已从「${target.title}」移除"
                            Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isIncluded) DragonTheme.primary.copy(alpha = 0.12f) else DragonTheme.glassSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isIncluded) DragonTheme.primary.copy(alpha = 0.65f) else DragonTheme.glassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isIncluded) DragonTheme.primary.copy(alpha = 0.22f) else DragonTheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = target.iconVector,
                                        contentDescription = null,
                                        tint = if (isIncluded) DragonTheme.primary else DragonTheme.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = target.title,
                                            color = if (isIncluded) DragonTheme.primary else DragonTheme.textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = if (isIncluded) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (target.isCustom) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = DragonCyan.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "自定义",
                                                    color = DragonCyan,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${target.stationCount} 个电台",
                                        color = DragonTheme.textSecondary,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            // Right Check or Add Badge
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isIncluded) DragonTheme.primary else DragonTheme.glassBorder.copy(alpha = 0.3f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIncluded) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (isIncluded) (if (DragonTheme.isDark) Color(0xFF070B12) else Color.White) else DragonTheme.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

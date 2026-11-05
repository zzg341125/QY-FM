package com.dragon.fm.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.ui.theme.DragonTheme

data class ScenarioPillItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val scenarioKey: String,
    val isCountryPicker: Boolean = false,
    val isLocal: Boolean = false
)

@Composable
fun ScenarioPillsRow(
    modifier: Modifier = Modifier,
    selectedScenario: String,
    localLabel: String,
    onSelectScenario: (String) -> Unit,
    onOpenCountryPicker: () -> Unit
) {
    val cleanLocal = localLabel.replace("📍", "").replace("本地", "").trim().let {
        if (it.isNotBlank()) "本地 • $it" else "本地电台"
    }

    val pills = listOf(
        ScenarioPillItem("recommend", "精选推荐", Icons.Outlined.AutoAwesome, "精选推荐"),
        ScenarioPillItem("local", cleanLocal, Icons.Outlined.NearMe, "本地电台", isLocal = true),
        ScenarioPillItem("commute", "潮流通勤", Icons.Outlined.DirectionsCar, "潮流通勤"),
        ScenarioPillItem("sleep", "晚间助眠", Icons.Outlined.Bedtime, "晚间助眠"),
        ScenarioPillItem("focus", "咖啡专注", Icons.Outlined.LocalCafe, "咖啡专注"),
        ScenarioPillItem("news", "资讯谈话", Icons.Outlined.Mic, "资讯谈话"),
        ScenarioPillItem("country", "更多国家", Icons.Outlined.Public, "更多国家", isCountryPicker = true)
    )

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(pills, key = { it.id }) { item ->
            val isSelected = when {
                item.isLocal -> selectedScenario.contains("本地")
                else -> selectedScenario.contains(item.title) || selectedScenario == item.scenarioKey
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = when {
                    isSelected -> DragonTheme.primary.copy(alpha = 0.16f)
                    else -> DragonTheme.glassSurface
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = when {
                        isSelected -> DragonTheme.primary.copy(alpha = 0.75f)
                        else -> DragonTheme.glassBorder
                    }
                ),
                modifier = Modifier.clickable {
                    if (item.isCountryPicker) {
                        onOpenCountryPicker()
                    } else if (item.isLocal) {
                        onSelectScenario("本地电台")
                    } else {
                        onSelectScenario(item.scenarioKey)
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (isSelected) DragonTheme.primary else DragonTheme.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = item.title,
                        color = if (isSelected) DragonTheme.primary else DragonTheme.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}


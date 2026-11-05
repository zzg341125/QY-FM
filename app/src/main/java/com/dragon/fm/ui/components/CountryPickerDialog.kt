package com.dragon.fm.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.ui.theme.DragonTheme

data class CountryItem(val code: String, val name: String, val query: String)

@Composable
fun CountryPickerDialog(
    onSelectCountry: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val countries = listOf(
        CountryItem("CN", "中国 (China)", "China"),
        CountryItem("JP", "日本 (Japan)", "Japan"),
        CountryItem("GB", "英国 (United Kingdom)", "United Kingdom"),
        CountryItem("US", "美国 (United States)", "United States"),
        CountryItem("DE", "德国 (Germany)", "Germany"),
        CountryItem("FR", "法国 (France)", "France"),
        CountryItem("CA", "加拿大 (Canada)", "Canada"),
        CountryItem("AU", "澳大利亚 (Australia)", "Australia")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DragonTheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = null,
                    tint = DragonTheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "选择全球电台区域",
                    color = DragonTheme.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(countries) { country ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DragonTheme.glassSurface,
                        border = BorderStroke(1.dp, DragonTheme.glassBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectCountry(country.name)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DragonTheme.primary.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, DragonTheme.primary.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = country.code,
                                    color = DragonTheme.primary,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = country.name,
                                color = DragonTheme.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

package com.dragon.fm.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.R
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonTheme

/**
 * 品牌与微信公众号推广卡片：品牌（泷芯码匠）+ 功能简介 + 公众号二维码 + 复制关注
 * 风格参考 DyDown 关于页：与其搬运，不如原创。
 */
@Composable
fun AboutBrandCard(
    modifier: Modifier = Modifier,
    showBrandHeader: Boolean = true
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DragonTheme.surface,
        border = BorderStroke(1.dp, DragonTheme.glassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showBrandHeader) {
                // 品牌行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.app_icon),
                        contentDescription = "LX FM Logo",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(13.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = "LX FM",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DragonTheme.textPrimary
                        )
                        Text(
                            text = "泷芯码匠出品",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DragonCyan
                        )
                    }
                }

                // 简介
                Text(
                    text = "收录全国优质广播电台与全球精选频道，支持流光氛围、极速秒开、智能测速与全天候后台播放。所有电台直连源头，纯粹沉浸，享受最真实的电波魅力。",
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = DragonTheme.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )

                HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)
            }

            // 公众号宣传区
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                // 二维码白底容器，确保深色主题下扫码识别率 100%
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.wechat_qrcode),
                        contentDescription = "泷芯码匠公众号二维码",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Text(
                    text = "泷芯码匠 · 微信公众号",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DragonTheme.textPrimary
                )

                Text(
                    text = "与其搬运，不如原创",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = DragonCyan
                )

                Text(
                    text = "扫码关注，获取更新与更多原创软件\n有任何想法？后台留言，你的点子可能就是下一个作品",
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    color = DragonTheme.textSecondary,
                    textAlign = TextAlign.Center
                )

                // 公众号与开源按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("WeChat Official Account", "泷芯码匠")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "已复制「泷芯码匠」到剪贴板，可打开微信搜索关注", Toast.LENGTH_LONG).show()
                        },
                        border = BorderStroke(1.dp, DragonTheme.primary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = DragonTheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "复制公众号",
                            color = DragonTheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gitee.com/app-finder/LX-FM"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "请在浏览器访问: https://gitee.com/app-finder/LX-FM", Toast.LENGTH_LONG).show()
                            }
                        },
                        border = BorderStroke(1.dp, DragonCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = DragonCyan
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Gitee 开源",
                            color = DragonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = "LX FM v1.0.0 · 泷芯码匠出品",
                fontSize = 11.sp,
                color = DragonTheme.textSecondary.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 关注公众号弹出对话框，支持从任何界面快捷调起展示
 */
@Composable
fun WechatQrDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DragonTheme.surface,
        title = {
            Text(
                text = "关于作者与公众号",
                color = DragonTheme.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            AboutBrandCard(
                showBrandHeader = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = DragonTheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

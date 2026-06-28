package com.liferlighdow.iteration.ui

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.liferlighdow.iteration.R

class PinShortcutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            finish()
            return
        }

        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val request = try {
            launcherApps.getPinItemRequest(intent)
        } catch (e: Exception) {
            null
        }

        if (request == null || !request.isValid) {
            finish()
            return
        }

        val shortcutInfo = request.shortcutInfo
        val label = shortcutInfo?.shortLabel ?: shortcutInfo?.longLabel ?: "Shortcut"
        val icon = shortcutInfo?.let { 
            launcherApps.getShortcutIconDrawable(it, resources.displayMetrics.densityDpi) 
        }

        setContent {
            IterationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = getString(R.string.app_name),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = stringResource(R.string.pin_shortcut_msg),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    icon?.let {
                                        Image(
                                            bitmap = it.toBitmap().asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = label.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = { finish() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text(stringResource(R.string.cancel))
                                }
                                
                                Button(
                                    onClick = {
                                        if (request.isValid) {
                                            request.accept()
                                            // 發送廣播通知 Launcher 重新整理 App 列表
                                            val refreshIntent = android.content.Intent("com.liferlighdow.iteration.ACTION_REFRESH_APPS")
                                            refreshIntent.setPackage(packageName)
                                            sendBroadcast(refreshIntent)
                                            finish()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text(stringResource(R.string.add_to_home))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

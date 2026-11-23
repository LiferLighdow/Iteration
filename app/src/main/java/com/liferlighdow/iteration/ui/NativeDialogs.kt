package com.liferlighdow.iteration.ui

import android.app.AlertDialog
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liferlighdow.iteration.R

/**
 * 顯示原生風格的卸載確認對話框
 * 使用 DeviceDefault 主題以模擬系統原生 APP 的卸載體驗
 */
fun showNativeUninstallDialog(
    context: Context,
    appName: String,
    onConfirm: () -> Unit
) {
    val dialog = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setTitle(context.getString(R.string.pwa_uninstall_title, appName))
        .setMessage(context.getString(R.string.pwa_uninstall_message))
        .setPositiveButton(context.getString(R.string.pwa_uninstall_ok)) { dialog, _ ->
            onConfirm()
            dialog.dismiss()
        }
        .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        .create()
    
    dialog.show()

    // 強制設定按鈕顏色為 Google 藍色
    val googleBlue = context.getColor(R.color.google_blue)
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(googleBlue)
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(googleBlue)
}

/**
 * Material 3 設計的精美對話框：詢問從桌面上移除或是解除安裝
 */
@Composable
fun RemoveOrUninstallDialog(
    appName: String,
    onDismiss: () -> Unit,
    onRemoveFromDesktop: () -> Unit,
    onUninstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = appName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.remove_or_uninstall_msg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Remove from Desktop
                Surface(
                    onClick = {
                        onRemoveFromDesktop()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.delete_home),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.delete_home_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Uninstall
                Surface(
                    onClick = {
                        onUninstall()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.menu_uninstall),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(R.string.uninstall_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

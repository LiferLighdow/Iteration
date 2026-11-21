package com.liferlighdow.iteration.ui

import android.app.AlertDialog
import android.content.Context
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
 * 顯示詢問是要從桌面上移除還是解除安裝對話框
 */
fun showRemoveOrUninstallDialog(
    context: Context,
    appName: String,
    onRemoveFromDesktop: () -> Unit,
    onUninstall: () -> Unit
) {
    val options = arrayOf(
        context.getString(R.string.delete_home),
        context.getString(R.string.menu_uninstall)
    )
    val dialog = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setTitle(appName)
        .setItems(options) { d, which ->
            when (which) {
                0 -> onRemoveFromDesktop()
                1 -> onUninstall()
            }
            d.dismiss()
        }
        .setNegativeButton(context.getString(R.string.cancel)) { d, _ ->
            d.dismiss()
        }
        .create()
    
    dialog.show()

    val googleBlue = context.getColor(R.color.google_blue)
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(googleBlue)
}

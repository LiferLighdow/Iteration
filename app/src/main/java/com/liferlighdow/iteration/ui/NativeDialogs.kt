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

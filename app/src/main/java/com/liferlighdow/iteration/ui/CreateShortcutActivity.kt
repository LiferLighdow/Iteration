package com.liferlighdow.iteration.ui

import android.app.Activity
import android.os.Bundle

/**
 * 空的 Activity 用於宣告桌面支援 CREATE_SHORTCUT，增加與 Hermit 等 App 的相容性。
 */
class CreateShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 暫時不需要實作內容，僅需存在即可
        finish()
    }
}

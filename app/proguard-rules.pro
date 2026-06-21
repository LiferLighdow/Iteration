# [Iteration Launcher] ProGuard/R8 configuration

# 1. 保持 ViewModel 構造函數，避免 ViewModelProvider 反射失敗
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# 2. 保持 Data Models，避免混淆導致的 JSON 解析或邏輯錯誤
-keep class com.liferlighdow.iteration.data.AppModel { *; }
-keep class com.liferlighdow.iteration.data.WidgetModel { *; }
-keep class com.liferlighdow.iteration.data.WidgetType { *; }
-keep class com.liferlighdow.iteration.data.WidgetDisplayMode { *; }
-keep class com.liferlighdow.iteration.utils.IconStyle { *; }
-keep class com.liferlighdow.iteration.data.CalendarEvent { *; }

# 3. 保持 Enum 的必要方法 (valueOf/values)，因為程式碼中有用到 valueOf
-keepclassmembers enum com.liferlighdow.iteration.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 4. 保持 NotificationService，系統需要透過類名啟動它
-keep class com.liferlighdow.iteration.service.NotificationService { *; }

# 5. 保持 Material Color Utilities，避免動態色彩提取失效或崩潰
-keep class com.google.android.material.color.utilities.** { *; }
-keep class com.materialkolor.** { *; }

# 6. Coil 圖片載入庫的混淆規則
-keep class coil.** { *; }
-dontwarn coil.**

# 7. 保持 Compose 的一些內部結構 (通常 AGP 會處理，但加強保險)
-keep class androidx.compose.material3.** { *; }

# 8. 避免混淆導致的行號丟失，便於分析 Release 崩潰日誌
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*

# 9. Backdrop 庫規則 (避免 R8 過度優化致效能下降或崩潰)
-keep class com.kyant.backdrop.** { *; }
-dontwarn com.kyant.backdrop.**

# 10. Device Admin
-keep class com.liferlighdow.iteration.service.AdminReceiver { *; }

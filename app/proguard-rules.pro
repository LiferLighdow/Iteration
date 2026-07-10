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

# 4. 保持 Service，系統需要透過類名啟動它
-keep class com.liferlighdow.iteration.service.NotificationService { *; }
-keep class com.liferlighdow.iteration.service.IterationAccessibilityService { *; }

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

# 10. Shizuku/Sui 規則 (修正 Release 版本中反射失敗的問題)
-keep class dev.rikka.shizuku.** { *; }
-keep interface dev.rikka.shizuku.** { *; }
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn dev.rikka.shizuku.**
-dontwarn rikka.shizuku.**
-dontwarn moe.shizuku.**

# 保持 Binder/AIDL 接口，避免跨進程通信 (IPC) 失敗
-keep public interface * extends android.os.IInterface
-keep class * implements android.os.IInterface { *; }
-keep class * extends android.os.Binder { *; }

# 保持 Parcelable 的 CREATOR 欄位 (Shizuku 傳輸數據需要)
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

